package com.capstone.Capstone_2.service.impl;

import com.capstone.Capstone_2.dto.SignUpDto;
import com.capstone.Capstone_2.entity.CreatorProfile;
import com.capstone.Capstone_2.entity.User;
import com.capstone.Capstone_2.entity.UserRole;
import com.capstone.Capstone_2.repository.UserRepository;
import com.capstone.Capstone_2.service.user.UserServiceImpl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import static org.mockito.ArgumentMatchers.any;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @InjectMocks
    private UserServiceImpl userService;

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;

    private SignUpDto signUpDto;

    @BeforeEach
    void setUp() {
        signUpDto = new SignUpDto();
        signUpDto.setEmail("newuser@example.com");
        signUpDto.setNickname("NewUser");
        signUpDto.setPassword("password123");
        signUpDto.setPasswordConfirm("password123");
    }

    @Test
    @DisplayName("신규 사용자 등록 (registerNewUser) - 성공")
    void registerNewUser_success() {
        // Given
        given(userRepository.findByEmail("newuser@example.com")).willReturn(Optional.empty());
        given(passwordEncoder.encode("password123")).willReturn("encodedPassword");
        given(userRepository.save(any(User.class))).willAnswer(invocation -> invocation.getArgument(0));

        // When
        User newUser = userService.registerNewUser(signUpDto);

        // Then
        assertThat(newUser).isNotNull();

        // 1. userRepository.save가 호출되었는지, 그리고 그 인자가 올바른지 확인
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();

        // 2. User 엔티티 필드 검증
        assertThat(savedUser.getEmail()).isEqualTo("newuser@example.com");
        assertThat(savedUser.getNickname()).isEqualTo("NewUser");
        assertThat(savedUser.getPasswordHash()).isEqualTo("encodedPassword");
        assertThat(savedUser.getProvider()).isEqualTo("local");
        assertThat(savedUser.getRole()).isEqualTo(UserRole.USER);
        assertThat(savedUser.getStatus()).isEqualTo("active");

        // 3. CreatorProfile이 올바르게 생성되고 연결되었는지 검증
        CreatorProfile profile = savedUser.getCreatorProfile();
        assertThat(profile).isNotNull();
        assertThat(profile.getUser()).isEqualTo(savedUser); // 양방향 참조 확인
        assertThat(profile.getDisplayName()).isEqualTo("NewUser"); // 초기 표시명은 닉네임
    }

    @Test
    @DisplayName("신규 사용자 등록 - 실패 (비밀번호 불일치)")
    void registerNewUser_fail_passwordMismatch() {
        // Given
        signUpDto.setPasswordConfirm("wrongPassword");

        // When & Then
        assertThatThrownBy(() -> userService.registerNewUser(signUpDto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("비밀번호가 일치하지 않습니다.");
    }

    @Test
    @DisplayName("신규 사용자 등록 - 실패 (이메일 중복)")
    void registerNewUser_fail_emailExists() {
        // Given
        given(userRepository.findByEmail("newuser@example.com")).willReturn(Optional.of(User.builder().build()));

        // When & Then
        assertThatThrownBy(() -> userService.registerNewUser(signUpDto))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("이미 사용 중인 이메일입니다.");
    }
}