/*
package com.capstone.Capstone_2.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class S3UploaderService {

    private final S3Client s3Client;

    @Value("${s3.bucket-name}")
    private String bucketName;

    */
/**
     * MultipartFile을 S3에 업로드하고 URL을 반환합니다.
     * @param file 업로드할 파일
     * @param dirName S3 버킷 내에 파일을 저장할 디렉토리 이름 (예: "profiles", "courses")
     * @return 업로드된 파일의 전체 URL
     * @throws IOException 파일 처리 중 오류 발생 시
     *//*

    public String upload(MultipartFile file, String dirName) throws IOException {
        if (file == null || file.isEmpty()) {
            return null;
        }

        // 파일 이름이 중복되지 않도록 UUID를 파일명 앞에 추가합니다.
        String originalFilename = file.getOriginalFilename();
        String storedFileName = dirName + "/" + UUID.randomUUID() + "-" + originalFilename;

        // S3에 업로드하기 위한 요청 객체를 생성합니다.
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(storedFileName)
                .contentType(file.getContentType())
                .contentLength(file.getSize())
                .build();

        // S3 클라이언트를 통해 파일을 업로드합니다.
        s3Client.putObject(putObjectRequest, RequestBody.fromBytes(file.getBytes()));

        // 업로드된 파일의 URL을 반환합니다.
        return s3Client.utilities().getUrl(builder -> builder.bucket(bucketName).key(storedFileName)).toString();
    }
}*/
