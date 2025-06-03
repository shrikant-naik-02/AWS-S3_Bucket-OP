package com.excelfore.aws.awstask.service;

import com.excelfore.aws.awstask.common.CommonAWSOp;
import com.excelfore.aws.awstask.dto.PresignedUrlResponse;
import com.excelfore.aws.awstask.exception.*;
import com.excelfore.aws.awstask.util.FileUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class S3ServiceV2 {

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final CommonAWSOp commonAWSOp;

    private static final String FOLDER_NAME = "myBucket/";

    @Value("${aws.s3.bucket}")
    private String bucket;

//    public List<String> listOfObjectKeyName(){
//
//        ListObjectsV2Request request = ListObjectsV2Request.builder()
//                .bucket(bucket)
//                .build();
//        ListObjectsV2Response response = s3Client.listObjectsV2(request);
//        log.info("Hi {} {} {}",response.name(), response.keyCount(), response.toString());
//        log.info(String.valueOf(response.keyCount()));
//        return response.contents().stream().map(S3Object::key).toList();
//
//    }

    public List<String> listOfObjectKeyName(Optional<String> startWith, Optional<String> endWith) {
        // Build request with prefix if available
        ListObjectsV2Request.Builder requestBuilder = ListObjectsV2Request.builder()
                .bucket(bucket);

        startWith.ifPresent(requestBuilder::prefix);

        ListObjectsV2Response response = s3Client.listObjectsV2(requestBuilder.build());

        List<String> keys = response.contents().stream()
                .map(S3Object::key)
                .filter(key -> endWith.map(key::endsWith).orElse(true)) // Still filter endsWith manually
                .toList();

        log.info("S3 list filtered - Prefix: '{}', Suffix: '{}', Returned: {}",
                startWith.orElse(""), endWith.orElse(""), keys.size());

        return keys;
    }

    public PresignedUrlResponse getValidPresignedUrlForUpload(MultipartFile file) {

        if (file == null || file.isEmpty()) {
           throw new EmptyFileException("File is null or empty");
        }

        if (file.getSize() > FileUtil.mbToBytes(1)) {
            throw new FileTooLargeException("File size exceeds 1MB limit");
        }

        String originalFileName = file.getOriginalFilename();

        assert originalFileName != null;
        if (!FileUtil.isFileNameValid(originalFileName)) {
            throw new InvalidFileNameException("Filename must contain at least one letter before the extension"); // checked
        }

        String hashHex = FileUtil.computeSHA256Hash(file);

        String key = FOLDER_NAME + hashHex;
        log.info("Generated S3 object key: {}", key);

        if (commonAWSOp.doesObjectExists(key)) {
            throw new FileAlreadyExistsException("File already exists with name: " + key);
        }

        String url = commonAWSOp.generatePresignedUrl(key, true);
        return new PresignedUrlResponse(
                "Upload",
                url,
                "5Min",
                true
        );

    }

    public String uploadFileWithPresign(MultipartFile file, String presignedUrl) {
        String currentFileHash = FileUtil.computeSHA256Hash(file);

        Map<String,String> urlData = FileUtil.extractFolderShaKeyAndObjName(presignedUrl);
        final String shaKey = urlData.get("shaKey");
        final String objName = urlData.get("objName");

        if (!currentFileHash.equalsIgnoreCase(shaKey)) {
            log.info("mismatch");
            throw new HashMismatchException("Hash mismatch — please upload the original file used to generate the URL.");
        }

        if (commonAWSOp.doesObjectExists(objName)) {
            throw new FileAlreadyExistsException("File already exists with name: " + objName);
        }

        commonAWSOp.uploadFileWithPresignedUrl(file, presignedUrl);

        return objName;
    }


    public PresignedUrlResponse getValidPresignedUrlForDownload(String objectName) {

        if (!commonAWSOp.doesObjectExists(objectName)) {
            log.info("File Not Exist There With Name {}", objectName);
            throw new FileAlreadyExistsException("File Not Present There with name: " + objectName);
        }

        String url = commonAWSOp.generatePresignedUrl(objectName, false);

        return new PresignedUrlResponse(
                "Download",
                url,
                "5Min", // Or your actual expiration time in seconds
                true
        );

    }


    public byte[] downloadFileWithPresign(String presignedUrl) {

        Map<String,String> urlData = FileUtil.extractFolderShaKeyAndObjName(presignedUrl);
        final String objName = urlData.get("objName");

        if (!commonAWSOp.doesObjectExists(objName)) {
            log.info("File Already There With Name {}", objName);
            throw new NoSuchFilePresent("File Not exists with name: " + objName);
        }

        return commonAWSOp.downloadFileWithPresignedUrl(presignedUrl);

    }

}
