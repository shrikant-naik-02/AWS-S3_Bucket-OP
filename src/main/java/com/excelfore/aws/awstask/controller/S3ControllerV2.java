package com.excelfore.aws.awstask.controller;

import com.excelfore.aws.awstask.dto.ApiResponse;
import com.excelfore.aws.awstask.dto.PresignedUrlResponse;
import com.excelfore.aws.awstask.exception.MultipleFileSelectionException;
import com.excelfore.aws.awstask.service.S3ServiceV2;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.Part;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
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
            @RequestParam("file") MultipartFile file,  HttpServletRequest request) {

        validateSingleFileUpload(request);
        PresignedUrlResponse response = s3ServiceV2.getValidPresignedUrlForUpload(file);
        return ResponseEntity.ok(new ApiResponse<>(response));
    }

    @PostMapping("/upload-file-using-presigned-url")
    public ResponseEntity<ApiResponse<String>> uploadFileUsingPresignedUrl(
            @RequestParam("file") MultipartFile file,
            @RequestParam("presignedUrl") String presignedUrl, HttpServletRequest request) {

        validateSingleFileUpload(request);
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
        log.debug("Downloading from presigned URL: {}", presignedUrl);

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

    private void validateSingleFileUpload(HttpServletRequest request) {
        try {
            long fileCount = request.getParts().stream()
                    .filter(part -> "file".equals(part.getName()) && part.getSize() > 0)
                    .count();

            if (fileCount > 1) {
                throw new MultipleFileSelectionException("Multiple files uploaded. Only one file is allowed.");
            }
        } catch (IOException | ServletException e) {
            throw new RuntimeException("Failed to parse request parts.", e);
        }
    }


}
