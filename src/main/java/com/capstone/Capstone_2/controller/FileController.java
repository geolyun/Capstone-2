/*
package com.capstone.Capstone_2.controller;

import com.capstone.Capstone_2.service.S3UploaderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileController {

    private final S3UploaderService s3UploaderService;

    */
/**
     * 파일 업로드를 처리하는 API 엔드포인트입니다.
     * @param file 클라이언트로부터 받은 'file'이라는 이름의 MultipartFile
     * @return 업로드 성공 시 S3 URL이 담긴 JSON, 실패 시 서버 에러 응답
     *//*

    @PostMapping("/upload")
    public ResponseEntity<Map<String, String>> uploadFile(@RequestParam("file") MultipartFile file) {
        try {
            // 파일을 S3의 'profiles' 디렉토리에 업로드합니다.
            String fileUrl = s3UploaderService.upload(file, "profiles");
            // {"url": "https://..."} 형태의 JSON으로 응답합니다.
            return ResponseEntity.ok(Map.of("url", fileUrl));
        } catch (IOException e) {
            // 업로드 중 에러 발생 시 로그를 남기고 500 에러를 응답합니다.
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }
}*/
