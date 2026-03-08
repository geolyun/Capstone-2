package com.capstone.Capstone_2.config;

import com.capstone.Capstone_2.dto.UserPrincipal;
import com.capstone.Capstone_2.entity.User;
import com.capstone.Capstone_2.entity.UserRole;
import io.jsonwebtoken.ExpiredJwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils; // ReflectionTestUtils 사용

import java.util.Date;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtUtilTest {

    private JwtUtil jwtUtil;

    // 테스트용 Secret Key (application.properties의 값과 동일하거나 테스트용 값)
    private final String testSecret = "eff663a9cf38d6a269b79ce3482ee86e1e281d4d9a4e236126fc325ad5355247";
    private UserDetails userDetails;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        // @Value("${jwt.secret}")를 ReflectionTestUtils를 사용해 수동 주입
        ReflectionTestUtils.setField(jwtUtil, "secret", testSecret);

        User testUser = User.builder()
                .email("test@example.com")
                .role(UserRole.USER)
                .build();
        userDetails = new UserPrincipal(testUser);
    }

    @Test
    @DisplayName("JWT 토큰 생성 및 사용자 이름 추출")
    void generateTokenAndExtractUsername() {
        // When
        String token = jwtUtil.generateToken(userDetails);

        // Then
        assertThat(token).isNotNull().isNotEmpty();
        String extractedUsername = jwtUtil.extractUsername(token);
        assertThat(extractedUsername).isEqualTo("test@example.com");
    }

    @Test
    @DisplayName("JWT 토큰 유효성 검증 - 성공")
    void isTokenValid_success() {
        // Given
        String token = jwtUtil.generateToken(userDetails);

        // When
        boolean isValid = jwtUtil.isTokenValid(token, userDetails);

        // Then
        assertThat(isValid).isTrue();
    }

    @Test
    @DisplayName("JWT 토큰 유효성 검증 - 실패 (다른 사용자)")
    void isTokenValid_fail_wrongUser() {
        // Given
        String token = jwtUtil.generateToken(userDetails);

        User otherUser = User.builder().email("other@example.com").role(UserRole.USER).build();
        UserDetails otherUserDetails = new UserPrincipal(otherUser);

        // When
        boolean isValid = jwtUtil.isTokenValid(token, otherUserDetails);

        // Then
        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("JWT 토큰 유효성 검증 - 실패 (만료된 토큰)")
    void isTokenValid_fail_expired() {
        // Given
        // 만료 시간을 0 (즉시 만료)으로 설정하여 토큰 생성
        ReflectionTestUtils.setField(jwtUtil, "JWT_EXPIRATION_MS", 0L);
        String expiredToken = jwtUtil.generateToken(userDetails);

        // 만료 시간 원복 (다른 테스트 영향 방지)
        ReflectionTestUtils.setField(jwtUtil, "JWT_EXPIRATION_MS", 1000 * 60 * 60 * 24L);


        // When & Then
        // isTokenValid는 만료 여부도 검사하므로 false 반환
        assertThat(jwtUtil.isTokenValid(expiredToken, userDetails)).isFalse();

        // extractClaim 호출 시 ExpiredJwtException 발생
        assertThatThrownBy(() -> jwtUtil.extractUsername(expiredToken))
                .isInstanceOf(ExpiredJwtException.class);
    }
}