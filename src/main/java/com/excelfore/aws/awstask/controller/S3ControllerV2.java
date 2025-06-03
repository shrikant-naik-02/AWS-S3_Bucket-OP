package com.excelfore.aws.awstask.controller;

import com.excelfore.aws.awstask.dto.ApiResponse;
import com.excelfore.aws.awstask.dto.PresignedUrlResponse;
import com.excelfore.aws.awstask.service.S3ServiceV2;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;

@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/api/v2/s3_bucket")
public class S3ControllerV2 {

    private final S3ServiceV2 s3ServiceV2;

    @PostMapping("/presigned-url")
    public ResponseEntity<ApiResponse<PresignedUrlResponse>> generatePresignedUrlForUpload(
            @RequestParam("file") MultipartFile file) {

        PresignedUrlResponse response = s3ServiceV2.getValidPresignedUrlForUpload(file);
        return ResponseEntity.ok(new ApiResponse<>(response));
    }

    @PostMapping("/upload-file-using-presigned-url")
    public ResponseEntity<ApiResponse<String>> uploadFileUsingPresignedUrl(
            @RequestParam("file") MultipartFile file,
            @RequestParam("presignedUrl") String presignedUrl) {

        String objName = s3ServiceV2.uploadFileWithPresign(file, presignedUrl);
        return ResponseEntity.ok(new ApiResponse<>("File " + objName + " Uploaded Successfully"));
    }

    @PostMapping("/download-presigned-url")
    public ResponseEntity<ApiResponse<PresignedUrlResponse>> generatePresignedUrlForDownload(
            @RequestParam("objectName") String objectName) {

        PresignedUrlResponse response = s3ServiceV2.getValidPresignedUrlForDownload(objectName);
        return ResponseEntity.ok(new ApiResponse<>(response));
    }

    @PostMapping("/download-file-using-presigned-url")
    public ResponseEntity<byte[]> downloadFileUsingPresigned(@RequestParam("presignedUrl") String presignedUrl) {
        log.info("Downloading from presigned URL: {}", presignedUrl);

        byte[] fileBytes = s3ServiceV2.downloadFileWithPresign(presignedUrl);
        return ResponseEntity.ok().body(fileBytes);
    }

    @GetMapping("/list")
    public ResponseEntity<List<String>> getAllObjectKeys(
            @RequestParam(required = false) String startWith,
            @RequestParam(required = false) String endWith) {

        log.debug("Fetching file names with filters: startWith='{}', endWith='{}'", startWith, endWith);

        List<String> filenames = s3ServiceV2.listOfObjectKeyName(
                Optional.ofNullable(startWith),
                Optional.ofNullable(endWith)
        );

        return ResponseEntity.ok(filenames);
    }

}
