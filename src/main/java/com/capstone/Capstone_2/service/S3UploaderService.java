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

    @Value("${app.aws.s3.base-dir:}") // 없으면 빈 문자열
    private String baseDir;

    public String upload(MultipartFile file, String dirName) throws IOException {
        if (file == null || file.isEmpty()) return null;

        String originalFilename = file.getOriginalFilename();
        String keyPrefix = (dirName != null && !dirName.isBlank()) ? dirName :
                (baseDir != null ? baseDir : "");
        keyPrefix = keyPrefix == null ? "" : keyPrefix.replaceAll("^/|/$", ""); // 앞뒤 슬래시 제거

        String key = (keyPrefix.isBlank() ? "" : keyPrefix + "/")
                + UUID.randomUUID() + "-" + (originalFilename == null ? "file" : originalFilename);

        PutObjectRequest put = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .contentType(file.getContentType())
                .contentLength(file.getSize())
                .build();

        s3Client.putObject(put, RequestBody.fromBytes(file.getBytes()));

        // 공개 버킷이라면 아래 URL로 바로 접근 가능
        return s3Client.utilities()
                .getUrl(b -> b.bucket(bucketName).key(key))
                .toString();
    }
}