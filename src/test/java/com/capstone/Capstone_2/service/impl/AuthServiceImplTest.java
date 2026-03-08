package com.capstone.Capstone_2.service.impl;

import com.capstone.Capstone_2.config.JwtUtil;
import com.capstone.Capstone_2.dto.LoginDto;
import com.capstone.Capstone_2.dto.SignUpDto;
import com.capstone.Capstone_2.dto.TokenDto;
import com.capstone.Capstone_2.entity.User;
import com.capstone.Capstone_2.repository.UserRepository;
import com.capstone.Capstone_2.service.auth.AuthServiceImpl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @InjectMocks
    private AuthServiceImpl authService;

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private JwtUtil jwtUtil;

    private SignUpDto signUpDto;

    @BeforeEach
    void setUp() {
        signUpDto = new SignUpDto();
        signUpDto.setEmail("test@example.com");
        signUpDto.setNickname("tester");
        signUpDto.setPassword("password123");
        signUpDto.setPasswordConfirm("password123");
    }

    @Test
    @DisplayName("회원가입 (signup) - 성공")
    void signup_success() {
        // Given
        given(userRepository.findByEmail("test@example.com")).willReturn(Optional.empty());
        given(passwordEncoder.encode("password123")).willReturn("encodedPassword");

        // When
        authService.signup(signUpDto);

        // Then
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();

        assertThat(savedUser.getEmail()).isEqualTo("test@example.com");
        assertThat(savedUser.getNickname()).isEqualTo("tester");
        assertThat(savedUser.getPasswordHash()).isEqualTo("encodedPassword");
        assertThat(savedUser.getProvider()).isEqualTo("local");
        assertThat(savedUser.getRole()).isEqualTo(User.builder().build().getRole()); // USER (기본값)
        assertThat(savedUser.getCreatorProfile()).isNotNull();
        assertThat(savedUser.getCreatorProfile().getDisplayName()).isEqualTo("tester");
        assertThat(savedUser.getCreatorProfile().getUser()).isEqualTo(savedUser);
    }

    @Test
    @DisplayName("회원가입 (signup) - 실패 (비밀번호 불일치)")
    void signup_fail_passwordMismatch() {
        // Given
        signUpDto.setPasswordConfirm("wrongPassword");

        // When & Then
        assertThatThrownBy(() -> authService.signup(signUpDto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("비밀번호가 일치하지 않습니다.");
    }

    @Test
    @DisplayName("회원가입 (signup) - 실패 (이메일 중복)")
    void signup_fail_emailExists() {
        // Given
        given(userRepository.findByEmail("newuser@example.com")).willReturn(Optional.of(User.builder().build()));

        // When & Then
        assertThatThrownBy(() -> authService.signup(signUpDto))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("이미 사용 중인 이메일입니다.");
    }

    @Test
    @DisplayName("로그인 (login) - 성공")
    void login_success() {
        // Given
        LoginDto loginDto = new LoginDto();
        // (LoginDto에 setter가 없으므로 ReflectionTestUtils 또는 생성자 사용 가정)
        // 여기서는 LoginDto에 Getter만 있고 필드 접근이 안되므로, DTO를 수정하거나
        // 테스트를 위해 DTO를 로컬에서 생성합니다.
        // LoginDto는 Getter만 있으므로, 실제로는 JSON 바인딩으로 생성됩니다.
        // 테스트에서는 loginDto.getEmail()을 모킹할 수 없으므로,
        // authenticationManager.authenticate 호출에 사용되는 Token을 검증합니다.
        // (실제로는 LoginDto에 email, password 필드가 있고 getter가 있다고 가정)
        // --> `src/main/java/com/capstone/Capstone_2/dto/LoginDto.java` 파일을 보니 getter만 있네요.
        // --> AuthServiceImpl 77라인 `new UsernamePasswordAuthenticationToken(loginDto.getEmail(), loginDto.getPassword())`
        // --> 이 코드가 동작하려면 LoginDto에 필드와 getter가 있어야 합니다. (파일 내용 확인: 있습니다. OK)

        // LoginDto 필드 설정 (테스트를 위해 Setter가 있다고 가정하거나, 리플렉션 사용)
        // 여기서는 LoginDto가 필드를 가지고 있고, Mockito가 아닌 실제 객체를 생성한다고 가정합니다.
        // (테스트 편의를 위해 LoginDto에 Setter를 추가하는 것이 좋습니다)
        // LoginDto에 Setter가 없으므로, AuthServiceImpl의 로직을 테스트하기 위해
        // LoginDto 대신 인증 토큰을 직접 생성하여 테스트합니다.
        // --> 아니요, `loginDto.getEmail()`을 호출하므로 `loginDto` 객체가 필요합니다.
        // --> `LoginDto.java`는 필드와 Getter만 있습니다. Setter가 없으므로 private 필드에 값을 주입해야 합니다.
        // --> ... 아, `LoginDto`는 그냥 Getter만 있는 DTO군요.
        // --> `AuthServiceImpl`은 `loginDto.getEmail()`을 호출합니다.
        // --> `LoginDto`에 email, password 필드가 private으로 선언되어 있고, Getter가 public입니다.
        // --> 테스트에서 `loginDto` 객체를 생성하고 필드 값을 설정해야 합니다.
        // --> Lombok @Getter는 필드가 private이어도 getter를 만들어줍니다.
        // --> private 필드에 값을 설정하려면?
        // 1. LoginDto에 @AllArgsConstructor 추가 (가장 좋음)
        // 2. ReflectionTestUtils 사용
        // 3. 테스트용 DTO 클래스 생성
        // 여기서는 ReflectionTestUtils를 사용하겠습니다.

        LoginDto testLoginDto = new LoginDto();
        org.springframework.test.util.ReflectionTestUtils.setField(testLoginDto, "email", "test@example.com");
        org.springframework.test.util.ReflectionTestUtils.setField(testLoginDto, "password", "password123");


        Authentication authInput = new UsernamePasswordAuthenticationToken("test@example.com", "password123");
        Authentication authResult = mock(Authentication.class);
        UserDetails userDetails = mock(UserDetails.class);

        given(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).willReturn(authResult);
        given(authResult.getPrincipal()).willReturn(userDetails);
        given(jwtUtil.generateToken(userDetails)).willReturn("test.jwt.token");

        // When
        TokenDto tokenDto = authService.login(testLoginDto);

        // Then
        assertThat(tokenDto).isNotNull();
        assertThat(tokenDto.getToken()).isEqualTo("test.jwt.token");

        // authenticate가 올바른 정보로 호출되었는지 확인
        ArgumentCaptor<UsernamePasswordAuthenticationToken> authCaptor = ArgumentCaptor.forClass(UsernamePasswordAuthenticationToken.class);
        verify(authenticationManager).authenticate(authCaptor.capture());
        assertThat(authCaptor.getValue().getName()).isEqualTo("test@example.com");
        assertThat(authCaptor.getValue().getCredentials()).isEqualTo("password123");
    }

    @Test
    @DisplayName("로그인 (login) - 실패 (자격 증명 실패)")
    void login_fail_badCredentials() {
        // Given
        LoginDto testLoginDto = new LoginDto();
        org.springframework.test.util.ReflectionTestUtils.setField(testLoginDto, "email", "test@example.com");
        org.springframework.test.util.ReflectionTestUtils.setField(testLoginDto, "password", "wrongpassword");

        given(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .willThrow(new BadCredentialsException("자격 증명 실패"));

        // When & Then
        assertThatThrownBy(() -> authService.login(testLoginDto))
                .isInstanceOf(BadCredentialsException.class);
    }
}