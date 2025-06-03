package com.excelfore.aws.awstask.controller;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;

import com.excelfore.aws.awstask.service.S3ServiceV1;

import java.util.UUID;

@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/api/s3_bucket")
public class S3ControllerV1 {

    private final S3ServiceV1 s3ServiceV1;

    @PostMapping("/upload")
    public ResponseEntity<String> upload(@RequestParam("file") MultipartFile file){

        String key = file.getOriginalFilename() + "-" + UUID.randomUUID() ;

        s3ServiceV1.uploadFile(key,file);
        log.debug("Controller S3Controller upload-1 Success FileName: {}", key);
        return ResponseEntity.ok("Uploaded with key: " + key);
    }

    @GetMapping("/download")
    public ResponseEntity<byte[]> download(@RequestParam String key){



        byte[] content = s3ServiceV1.downloadFile(key);
        log.debug("Controller S3Controller download-1 variable-content: {}", content);

        // Allow Only For Download No Preview
        HttpHeaders headers = new HttpHeaders();
        headers.setContentDisposition(ContentDisposition.attachment().filename(key).build());
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM); // safe fallback

        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\""+key+"\"")
                .body(content);
    }

}
