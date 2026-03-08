package com.capstone.Capstone_2.service;

import com.capstone.Capstone_2.dto.UserPrincipal;
import com.capstone.Capstone_2.entity.User;
import com.capstone.Capstone_2.entity.UserRole;
import com.capstone.Capstone_2.repository.UserRepository;
import com.capstone.Capstone_2.service.user.CustomUserDetailsService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @InjectMocks
    private CustomUserDetailsService customUserDetailsService;

    @Mock
    private UserRepository userRepository;

    private User testUser;
    private String userEmail = "user@example.com";

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .email(userEmail)
                .nickname("tester")
                .passwordHash("encodedPassword")
                .role(UserRole.USER)
                .status("active")
                .build();
    }

    @Test
    @DisplayName("사용자 조회 (loadUserByUsername) - 성공")
    void loadUserByUsername_success() {
        // Given
        given(userRepository.findByEmail(userEmail)).willReturn(Optional.of(testUser));

        // When
        UserDetails userDetails = customUserDetailsService.loadUserByUsername(userEmail);

        // Then
        assertThat(userDetails).isInstanceOf(UserPrincipal.class);
        assertThat(userDetails.getUsername()).isEqualTo(userEmail);
        assertThat(userDetails.getPassword()).isEqualTo("encodedPassword");
        assertThat(userDetails.getAuthorities()).hasSize(1);
        assertThat(userDetails.getAuthorities().iterator().next().getAuthority()).isEqualTo("ROLE_USER");
        assertThat(userDetails.isEnabled()).isTrue();
    }

    @Test
    @DisplayName("사용자 조회 (loadUserByUsername) - 실패 (사용자 없음)")
    void loadUserByUsername_fail_notFound() {
        // Given
        given(userRepository.findByEmail(userEmail)).willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> customUserDetailsService.loadUserByUsername(userEmail))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessage("해당 이메일을 가진 사용자를 찾을 수 없습니다: " + userEmail);
    }

    @Test
    @DisplayName("사용자 조회 (loadUserByUsername) - 비활성화 (isEnabled=false)")
    void loadUserByUsername_disabledUser() {
        // Given
        testUser.setStatus("SUSPENDED"); // 비활성 상태
        given(userRepository.findByEmail(userEmail)).willReturn(Optional.of(testUser));

        // When
        UserDetails userDetails = customUserDetailsService.loadUserByUsername(userEmail);

        // Then
        assertThat(userDetails.isEnabled()).isFalse();
    }
}