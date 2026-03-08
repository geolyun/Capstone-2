package com.capstone.Capstone_2.service;

import com.capstone.Capstone_2.dto.UserPrincipal;
import com.capstone.Capstone_2.entity.User;
import com.capstone.Capstone_2.entity.UserRole;
import com.capstone.Capstone_2.repository.UserRepository;
import com.capstone.Capstone_2.service.user.CustomOAuth2UserService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomOAuth2UserServiceTest {

    // @InjectMocks는 final 필드(userRepository) 주입이 어려울 수 있으므로 수동 생성
    private CustomOAuth2UserService customOAuth2UserService;

    @Mock
    private UserRepository userRepository;

    // DefaultOAuth2UserService의 super.loadUser()를 모킹하기 위해 @Spy 대신 @Mock 사용
    @Mock
    private DefaultOAuth2UserService defaultOAuth2UserService;

    @Mock
    private OAuth2UserRequest userRequest;
    @Mock
    private ClientRegistration clientRegistration;
    @Mock
    private ClientRegistration.ProviderDetails providerDetails;
    @Mock
    private ClientRegistration.ProviderDetails.UserInfoEndpoint userInfoEndpoint;

    private Map<String, Object> googleAttributes;

    @BeforeEach
    void setUp() {
        // @InjectMocks 대신 수동 주입 및 Spy 설정
        customOAuth2UserService = new CustomOAuth2UserService(userRepository);
        // super.loadUser() 호출을 모킹하기 위해 defaultOAuth2UserService를 사용 (Spy 대체)
        // CustomOAuth2UserService는 DefaultOAuth2UserService를 상속하므로,
        // super.loadUser()를 모킹하는 것은 까다롭습니다.
        // 여기서는 CustomOAuth2UserService의 `loadUser` 메서드 자체를 테스트하되,
        // `super.loadUser` 부분을 모킹합니다.
        // -> @InjectMocks를 사용하고, DefaultOAuth2UserService를 @Spy로 선언하여 super.loadUser를 모킹할 수 있습니다.
        // -> 하지만 @Spy는 실제 객체를 생성하므로, @Mock으로 DefaultOAuth2UserService를 만들고
        //    CustomOAuth2UserService의 생성자에 userRepository만 주입하는 것이 더 간단합니다.
        // -> CustomOAuth2UserService가 DefaultOAuth2UserService를 상속하므로,
        //    @InjectMocks를 사용하면 userRepository가 주입됩니다.
        //    `super.loadUser`를 모킹하기 위해 `@Spy`를 주입 시도

        // --> 스파이 대신, CustomOAuth2UserService 자체를 스파이로 만듭니다.
        customOAuth2UserService = spy(new CustomOAuth2UserService(userRepository));


        googleAttributes = Map.of(
                "name", "Test User",
                "email", "google@example.com",
                "picture", "http://google.com/pic.png",
                "sub", "google_12345"
        );
    }

    private void mockUserRequest(String registrationId, String userNameAttributeName) {
        given(userRequest.getClientRegistration()).willReturn(clientRegistration);
        given(clientRegistration.getRegistrationId()).willReturn(registrationId);
        given(clientRegistration.getProviderDetails()).willReturn(providerDetails);
        given(providerDetails.getUserInfoEndpoint()).willReturn(userInfoEndpoint);
        given(userInfoEndpoint.getUserNameAttributeName()).willReturn(userNameAttributeName);
    }

    @Test
    @DisplayName("OAuth2 신규 사용자 로그인 (saveOrUpdate) - GUEST로 저장")
    void loadUser_newUser_shouldSaveAsGuest() {
        // Given
        mockUserRequest("google", "sub");
        OAuth2User oAuth2User = new DefaultOAuth2User(List.of(), googleAttributes, "sub");

        // super.loadUser() 호출을 모의
        doReturn(oAuth2User).when((DefaultOAuth2UserService) customOAuth2UserService).loadUser(userRequest);

        given(userRepository.findByEmail("google@example.com")).willReturn(Optional.empty());
        given(userRepository.save(any(User.class))).willAnswer(invocation -> invocation.getArgument(0));

        // When
        OAuth2User result = customOAuth2UserService.loadUser(userRequest);

        // Then
        assertThat(result).isInstanceOf(UserPrincipal.class);
        assertThat(result.getAttributes()).isEqualTo(googleAttributes);
        assertThat(((UserPrincipal) result).getEmail()).isEqualTo("google@example.com");

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();

        assertThat(savedUser.getRole()).isEqualTo(UserRole.GUEST); // 신규 유저는 GUEST
        assertThat(savedUser.getProvider()).isEqualTo("google");
        assertThat(savedUser.getProviderId()).isEqualTo("google_12345");
        assertThat(savedUser.getCreatorProfile()).isNotNull();
        assertThat(savedUser.getCreatorProfile().getDisplayName()).isEqualTo("Test User");
    }

    @Test
    @DisplayName("OAuth2 기존 사용자 로그인 (saveOrUpdate) - 정보 업데이트 안 함")
    void loadUser_existingUser_shouldNotOverwrite() {
        // Given
        mockUserRequest("google", "sub");
        OAuth2User oAuth2User = new DefaultOAuth2User(List.of(), googleAttributes, "sub");

        User existingUser = User.builder()
                .email("google@example.com")
                .nickname("MyCustomNickname")
                .role(UserRole.USER) // 이미 USER 역할
                .provider("google")
                .providerId("google_12345")
                .build();

        doReturn(oAuth2User).when((DefaultOAuth2UserService) customOAuth2UserService).loadUser(userRequest);
        given(userRepository.findByEmail("google@example.com")).willReturn(Optional.of(existingUser));
        given(userRepository.save(any(User.class))).willAnswer(invocation -> invocation.getArgument(0));

        // When
        OAuth2User result = customOAuth2UserService.loadUser(userRequest);

        // Then
        assertThat(result).isInstanceOf(UserPrincipal.class);
        assertThat(((UserPrincipal) result).getUser()).isEqualTo(existingUser);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();

        // 닉네임이나 역할이 덮어쓰여지지 않았는지 확인
        assertThat(savedUser.getNickname()).isEqualTo("MyCustomNickname");
        assertThat(savedUser.getRole()).isEqualTo(UserRole.USER);
    }

    @Test
    @DisplayName("OAuth2 로그인 - 실패 (이메일 없음)")
    void loadUser_fail_noEmail() {
        // Given
        mockUserRequest("google", "sub");
        Map<String, Object> noEmailAttributes = Map.of("name", "Test User", "sub", "google_12345");
        OAuth2User oAuth2User = new DefaultOAuth2User(List.of(), noEmailAttributes, "sub");

        doReturn(oAuth2User).when((DefaultOAuth2UserService) customOAuth2UserService).loadUser(userRequest);

        // When & Then
        assertThatThrownBy(() -> customOAuth2UserService.loadUser(userRequest))
                .isInstanceOf(OAuth2AuthenticationException.class)
                .hasMessage("Email not found from OAuth2 provider.");
    }
}