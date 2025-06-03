package com.excelfore.aws.awstask.service;

import com.excelfore.aws.awstask.common.CommonAWSOp;
import com.excelfore.aws.awstask.exception.FileAlreadyExistsException;
import com.excelfore.aws.awstask.exception.FileDownloadException;
import com.excelfore.aws.awstask.exception.FileUploadException;

import com.excelfore.aws.awstask.exception.NoSuchFilePresent;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.beans.factory.annotation.Value;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;

import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

import java.io.IOException;

@Service
@RequiredArgsConstructor
@Slf4j
public class S3ServiceV1 {

    private final S3Client s3Client;
    private final CommonAWSOp commonAWSOp;

    @Value("${aws.s3.bucket}")
    private String bucket;

    public void uploadFile(String key, MultipartFile file){
        try{
            if(commonAWSOp.doesObjectExists(key)){
//            return ResponseEntity.status(HttpStatus.NOT_FOUND)
//                    .body(("File Not Found: "+key).getBytes(StandardCharsets.UTF_8));
                throw new FileAlreadyExistsException("File already exists with name: " + key);
        }


            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .contentType(file.getContentType())
                    .build();
            s3Client.putObject(request, RequestBody.fromBytes(file.getBytes()));
            log.debug("Service S3Service uploadFile-1 Success");
        }catch(IOException e){
            log.error("Service {} uploadFile-1 IOException. Message: {}", this.getClass().getSimpleName(), e.getMessage());
            throw new FileUploadException("Failed to upload file: " + e.getMessage());
        }
        catch (S3Exception e){
            log.error("Service {} uploadFile-2 S3Exception. Message: {}", this.getClass().getSimpleName(), e.getMessage());
            throw new FileUploadException("S3 error: " + e.getMessage());
        }
    }

    public byte[] downloadFile(String key) {

        if(!commonAWSOp.doesObjectExists(key)){
//            return ResponseEntity.status(HttpStatus.NOT_FOUND)
//                    .body(("File Not Found: "+key).getBytes(StandardCharsets.UTF_8));
            throw new NoSuchFilePresent("File already exists with name: " + key);
        }

        GetObjectRequest request = GetObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .build();
        try (ResponseInputStream<GetObjectResponse> response = s3Client.getObject(request)) {
            log.debug("Service S3Service downloadFile-1 Success");
            return response.readAllBytes();
        } catch (IOException e) {
            log.error("Service {} downloadFile-1 IOException. Message: {}", this.getClass().getSimpleName(), e.getMessage());
            throw new FileDownloadException("Failed to download file: " + e.getMessage());
        } catch (S3Exception e) {
            log.error("Service {} downloadFile-2 S3Exception. Message: {}", this.getClass().getSimpleName(), e.getMessage());
            throw new FileDownloadException("S3 error: " + e.awsErrorDetails().errorMessage());
        }
    }
}
