package com.capstone.Capstone_2.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Utilities;
import software.amazon.awssdk.services.s3.model.GetUrlRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

import java.io.IOException;
import java.net.URL;
import java.util.UUID;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class S3UploaderServiceTest {

    @InjectMocks
    private S3UploaderService s3UploaderService;

    @Mock
    private S3Client s3Client;

    @Mock
    private MultipartFile mockFile;

    @Mock
    private S3Utilities s3Utilities;

    @Mock
    private URL mockUrl;

    private String bucketName = "test-bucket";
    private String baseDir = "images";
    private String fileUrl = "http://s3.test-bucket.com/images/test_image.png";

    @BeforeEach
    void setUp() {
        // @Value 필드 주입
        ReflectionTestUtils.setField(s3UploaderService, "bucketName", bucketName);
        ReflectionTestUtils.setField(s3UploaderService, "baseDir", baseDir);
    }

    @Test
    @DisplayName("S3 파일 업로드 - 성공")
    void Supload_success() throws IOException {
        // Given
        byte[] fileContent = "test content".getBytes();
        String originalFilename = "test_image.png";

        given(mockFile.isEmpty()).willReturn(false);
        given(mockFile.getOriginalFilename()).willReturn(originalFilename);
        given(mockFile.getContentType()).willReturn("image/png");
        given(mockFile.getSize()).willReturn((long) fileContent.length);
        given(mockFile.getBytes()).willReturn(fileContent);

        given(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .willReturn(PutObjectResponse.builder().build());

        // s3Client.utilities().getUrl(...) 모킹
        given(s3Client.utilities()).willReturn(s3Utilities);
        given(s3Utilities.getUrl(any(Consumer.class))).willAnswer(invocation -> {
            Consumer<GetUrlRequest.Builder> consumer = invocation.getArgument(0);
            GetUrlRequest.Builder builder = GetUrlRequest.builder();
            consumer.accept(builder);
            // GetUrlRequest req = builder.build(); // 실제 URL 생성 로직 대신 mockUrl 반환
            return mockUrl;
        });
        given(mockUrl.toString()).willReturn(fileUrl);


        // When
        String resultUrl = s3UploaderService.upload(mockFile, "images"); // "images" 디렉토리 지정

        // Then
        assertThat(resultUrl).isEqualTo(fileUrl);

        // putObject가 올바른 인자와 함께 호출되었는지 검증
        ArgumentCaptor<PutObjectRequest> requestCaptor = ArgumentCaptor.forClass(PutObjectRequest.class);
        ArgumentCaptor<RequestBody> bodyCaptor = ArgumentCaptor.forClass(RequestBody.class);
        verify(s3Client).putObject(requestCaptor.capture(), bodyCaptor.capture());

        PutObjectRequest capturedRequest = requestCaptor.getValue();
        assertThat(capturedRequest.bucket()).isEqualTo(bucketName);
        assertThat(capturedRequest.key()).startsWith(baseDir + "/"); // baseDir(images)로 시작
        assertThat(capturedRequest.key()).endsWith(originalFilename);
        assertThat(capturedRequest.contentType()).isEqualTo("image/png");
        assertThat(capturedRequest.contentLength()).isEqualTo(fileContent.length);

        assertThat(bodyCaptor.getValue().contentLength()).isEqualTo(fileContent.length);
    }

    @Test
    @DisplayName("S3 파일 업로드 - 실패 (파일이 비어있음)")
    void upload_fail_fileIsEmpty() throws IOException {
        // Given
        given(mockFile.isEmpty()).willReturn(true);

        // When
        String resultUrl = s3UploaderService.upload(mockFile, "images");

        // Then
        assertThat(resultUrl).isNull();
        verify(s3Client, never()).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    @Test
    @DisplayName("S3 파일 업로드 - 실패 (IOException)")
    void upload_fail_ioException() throws IOException {
        // Given
        given(mockFile.isEmpty()).willReturn(false);
        given(mockFile.getOriginalFilename()).willReturn("test.png");
        given(mockFile.getBytes()).willThrow(new IOException("Read error"));

        // When & Then
        assertThatThrownBy(() -> s3UploaderService.upload(mockFile, "images"))
                .isInstanceOf(IOException.class)
                .hasMessage("Read error");
    }
}