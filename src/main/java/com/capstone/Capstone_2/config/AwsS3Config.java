package com.capstone.Capstone_2.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

@Configuration
public class AwsS3Config {

    @Value("${spring.cloud.aws.credentials.access-key}")
    private String accessKey;

    @Value("${spring.cloud.aws.credentials.secret-key}")
    private String secretKey;

    @Bean
    public S3Client s3Client(
            @Value("${app.aws.region:${AWS_REGION:ap-northeast-2}}") String region
    ) {
        // 주입받은 키를 사용하여 AWS 자격 증명 객체 생성
        AwsBasicCredentials credentials = AwsBasicCredentials.create(accessKey, secretKey);

        return S3Client.builder()
                .region(Region.of(region))
                // StaticCredentialsProvider를 사용하여 명시적으로 자격 증명 제공
                .credentialsProvider(StaticCredentialsProvider.create(credentials))
                .build();
    }
}