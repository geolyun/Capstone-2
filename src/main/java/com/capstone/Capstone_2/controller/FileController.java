package com.capstone.Capstone_2.controller;

import com.capstone.Capstone_2.service.S3UploaderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileController {

    private final S3UploaderService s3UploaderService;

    @PostMapping("/upload")
    public ResponseEntity<Map<String, String>> uploadFile(@RequestParam("file") MultipartFile file) {
        try {
            String fileUrl = s3UploaderService.upload(file, "images");
            if (fileUrl == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "파일이 비어있습니다."));
            }
            return ResponseEntity.ok(Map.of("url", fileUrl));
        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(Map.of("error", "파일 업로드 중 오류 발생"));
        }
    }
}
