package com.capstone.Capstone_2.service.impl;

import com.capstone.Capstone_2.dto.CreatorProfileDto;
import com.capstone.Capstone_2.dto.UserPrincipal;
import com.capstone.Capstone_2.entity.CreatorProfile;
import com.capstone.Capstone_2.entity.User;
import com.capstone.Capstone_2.repository.UserRepository;
import com.capstone.Capstone_2.service.mypage.CreatorProfileServiceImpl;

import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CreatorProfileServiceImplTest {

    @InjectMocks
    private CreatorProfileServiceImpl creatorProfileService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private SecurityContext securityContext;
    @Mock
    private Authentication authentication;

    private User testUser;
    private CreatorProfile testProfile;
    private String userEmail = "user@example.com";
    private CreatorProfileDto.UpdateRequest updateRequest;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .email(userEmail)
                .nickname("OldNickname")
                .avatarUrl("old_avatar.png")
                .build();
        testProfile = CreatorProfile.builder()
                .user(testUser)
                .displayName("OldDisplayName")
                .bio("Old Bio")
                .build();
        testUser.setCreatorProfile(testProfile);

        updateRequest = new CreatorProfileDto.UpdateRequest();
        updateRequest.setDisplayName("NewDisplayName");
        updateRequest.setBio("New Bio");
        updateRequest.setAvatarUrl("new_avatar.png");
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void mockSecurityContext() {
        UserPrincipal principal = new UserPrincipal(testUser);
        given(authentication.getPrincipal()).willReturn(principal);
        given(authentication.getCredentials()).willReturn("password");
        given(securityContext.getAuthentication()).willReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
    }

    @Test
    @DisplayName("프로필 수정 - 성공")
    void updateProfile_success() {
        // Given
        given(userRepository.findByEmail(userEmail)).willReturn(Optional.of(testUser));
        mockSecurityContext(); // SecurityContext 모의

        // When
        creatorProfileService.updateProfile(userEmail, updateRequest);

        // Then
        // 1. 엔티티 변경 확인
        assertThat(testProfile.getDisplayName()).isEqualTo("NewDisplayName");
        assertThat(testProfile.getBio()).isEqualTo("New Bio");
        assertThat(testUser.getAvatarUrl()).isEqualTo("new_avatar.png");

        // 2. SecurityContext가 갱신되었는지 확인
        ArgumentCaptor<Authentication> authCaptor = ArgumentCaptor.forClass(Authentication.class);
        verify(securityContext).setAuthentication(authCaptor.capture());

        // 3. 갱신된 Authentication 객체 내부의 Principal 확인
        Authentication newAuth = authCaptor.getValue();
        assertThat(newAuth).isInstanceOf(UsernamePasswordAuthenticationToken.class);
        UserPrincipal newPrincipal = (UserPrincipal) newAuth.getPrincipal();

        // 4. 갱신된 Principal이 변경된 User 정보를 반영하는지 확인
        assertThat(newPrincipal.getUser()).isEqualTo(testUser);
        assertThat(newPrincipal.getUser().getAvatarUrl()).isEqualTo("new_avatar.png");
        // UserPrincipal은 User 객체를 직접 참조하므로,
        // newPrincipal.getUser().getCreatorProfile().getDisplayName()은 자동으로 "NewDisplayName"이 됩니다.
        assertThat(newPrincipal.getUser().getCreatorProfile().getDisplayName()).isEqualTo("NewDisplayName");
    }

    @Test
    @DisplayName("프로필 수정 - 성공 (아바타 URL이 null 또는 blank인 경우)")
    void updateProfile_success_avatarUrlNullOrBlank() {
        // Given
        given(userRepository.findByEmail(userEmail)).willReturn(Optional.of(testUser));
        updateRequest.setAvatarUrl(null); // Avatar URL을 null로 설정

        // When
        creatorProfileService.updateProfile(userEmail, updateRequest);

        // Then
        // 다른 필드는 변경
        assertThat(testProfile.getDisplayName()).isEqualTo("NewDisplayName");
        // Avatar URL은 변경되지 않음 (기존값 유지)
        assertThat(testUser.getAvatarUrl()).isEqualTo("old_avatar.png");

        // Given 2: Avatar URL을 blank로 설정
        updateRequest.setAvatarUrl("   ");

        // When 2
        creatorProfileService.updateProfile(userEmail, updateRequest);

        // Then 2
        // Avatar URL은 여전히 변경되지 않음
        assertThat(testUser.getAvatarUrl()).isEqualTo("old_avatar.png");
    }

    @Test
    @DisplayName("프로필 수정 - 실패 (사용자 없음)")
    void updateProfile_fail_userNotFound() {
        // Given
        given(userRepository.findByEmail(userEmail)).willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> creatorProfileService.updateProfile(userEmail, updateRequest))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("사용자를 찾을 수 없습니다.");
    }

    @Test
    @DisplayName("프로필 수정 - 실패 (프로필 없음)")
    void updateProfile_fail_profileNotFound() {
        // Given
        testUser.setCreatorProfile(null); // 프로필 제거
        given(userRepository.findByEmail(userEmail)).willReturn(Optional.of(testUser));

        // When & Then
        assertThatThrownBy(() -> creatorProfileService.updateProfile(userEmail, updateRequest))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("크리에이터 프로필을 찾을 수 없습니다.");
    }
}