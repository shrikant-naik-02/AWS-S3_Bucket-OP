package com.excelfore.aws.awstask.common;

import com.excelfore.aws.awstask.exception.EmptyFileException;
import com.excelfore.aws.awstask.exception.PresignedUrlExpiredException;

import com.excelfore.aws.awstask.model.File;
import com.excelfore.aws.awstask.repository.FileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.beans.factory.annotation.Value;

import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;

@Component
@RequiredArgsConstructor
@Slf4j
public class CommonAWSOp {

    private final S3Presigner s3Presigner;
    private final S3Client s3Client;

    @Value("${aws.s3.bucket}")
    private String bucket;

    private final FileRepository fileRepository;


    public boolean doesObjectExists(String key){

        try{
            HeadObjectRequest headObjectRequest = HeadObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .build();

            s3Client.headObject(headObjectRequest);
            // At this line status code - 200 Only
            return true;

        } catch (NoSuchKeyException e) {
            int statusCode = e.statusCode();
            String requestId = e.requestId();
            log.debug("Error Failed To Get The Keys In Bucket. Class {} and ErrorMessage: {} StatusCode: {}, RequestId: {}", this.getClass().getSimpleName(), e.getMessage(), statusCode, requestId);
            return false;

        } catch (S3Exception e) {
            int statusCode = e.statusCode();
            String requestId = e.requestId();
            log.error("S3Exception occurred In {}. ErrorMessage: {}. StatusCode: {}, RequestId: {}", this.getClass().getSimpleName(), e.getMessage(),statusCode, requestId);
            throw new RuntimeException("S3 error: " + e.awsErrorDetails().errorMessage());

        }

    }

    public String generatePresignedUrl(String key, boolean isUpload){

        Duration expiration = Duration.ofMinutes(5);

        if(isUpload){
            PutObjectRequest putRequest = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .build();

            PresignedPutObjectRequest presigned = s3Presigner.presignPutObject(
                    PutObjectPresignRequest.builder()
                            .putObjectRequest(putRequest)
                            .signatureDuration(expiration)
                            .build()
            );

            if (presigned == null || presigned.url() == null) {
                throw new RuntimeException("Failed to generate presigned URL.");
            }
            if (presigned.expiration().isBefore(Instant.now())) {
                throw new RuntimeException("Presigned URL is already expired.");
            }

            return presigned.url().toString();

        }
        else{

            GetObjectRequest getRequest = GetObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .responseContentDisposition("attachment; filename=\"" + key + "\"")
                    .build();

            PresignedGetObjectRequest presigned = s3Presigner.presignGetObject(
                    GetObjectPresignRequest.builder()
                            .getObjectRequest(getRequest)
                            .signatureDuration(expiration)
                            .build()
            );

            if (presigned == null || presigned.url() == null) {
                throw new RuntimeException("Failed to generate presigned URL.");
            }
            if (presigned.expiration().isBefore(Instant.now())) {
                throw new RuntimeException("Presigned URL is already expired.");
            }

            return presigned.url().toString();

        }
    }

    public void uploadFileWithPresignedUrl(MultipartFile file, String presignedUrl, String objName) {

        // Have To Check The Logic That Presigned Url Expired Or Not

        try (HttpClient client = HttpClient.newHttpClient()) {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(presignedUrl))
                    .PUT(HttpRequest.BodyPublishers.ofByteArray(file.getBytes()))
                    .header("Content-Type", file.getContentType())
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            log.debug("Response status code: {}", response.statusCode());

            if (response.statusCode() == 403) {
                log.warn("Presigned URL has expired.");
                throw new PresignedUrlExpiredException("Presigned URL Get expired or URL get manipulated");
            }

            if (response.statusCode() == 200) {
                // Save to DB
                final String originalFilename = file.getOriginalFilename();

                File fileRecord = new File();
                fileRecord.setFileName(originalFilename);
                fileRecord.setAwsFileName(objName);
                fileRecord.setDownloadCount(0); // Initial count

                fileRepository.save(fileRecord);
                return;
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Upload interrupted. Message: {}", e.getMessage());
            throw new RuntimeException("Upload interrupted", e);

        } catch (IOException e) {
            log.error("I/O error during upload. Message: {}", e.getMessage());
            throw new RuntimeException("Upload failed due to I/O error", e);

        }
    }


    public byte[] downloadFileWithPresignedUrl(String presignedUrl, String objName) {

        try (HttpClient client = HttpClient.newHttpClient()) {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(presignedUrl))
                    .GET()
                    .build();

            HttpResponse<byte[]> response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());

            log.debug("Response status code: {}", response.statusCode());
            if (response.statusCode() == 403) {
                log.warn("Presigned URL has expired.");
                throw new PresignedUrlExpiredException("Presigned URL Get expired Or Url Get Manipulated");
            }

            if (response.statusCode() != 200) {
                log.error("Failed to download file. Status: {}", response.statusCode());
                throw new RuntimeException("Failed to download file, status code: " + response.statusCode());

            }

            // Now safe to assume body contains file bytes
            byte[] fileBytes = response.body();

            if (fileBytes == null || fileBytes.length == 0) {
                log.warn("Downloaded file is empty.");
                throw new EmptyFileException("Downloaded file is empty");
            }

            // Update download count in DB
            fileRepository.findByAwsFileName(objName)
                    .ifPresentOrElse(
                            fileMetadata -> {
                                fileMetadata.setDownloadCount(fileMetadata.getDownloadCount() + 1);
                                fileRepository.save(fileMetadata);
                                log.info("Download count incremented for object: {}", objName);
                            },
                            () -> log.warn("No DB record found for object: {}", objName)
                    );

            return fileBytes;

        }
        catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Download interrupted. Message: {}", e.getMessage());
            throw new RuntimeException("Download interrupted", e);

        } catch (IOException e) {
            log.error("I/O error during download. Message: {}", e.getMessage());
            throw new RuntimeException("Download failed due to I/O error", e);

        }
    }

}
