package com.capstone.Capstone_2.controller;

import com.capstone.Capstone_2.config.JwtUtil;
import com.capstone.Capstone_2.config.OAuth2SuccessHandler;
import com.capstone.Capstone_2.config.SecurityConfig;
import com.capstone.Capstone_2.dto.LikeDto;
import com.capstone.Capstone_2.dto.UserPrincipal;
import com.capstone.Capstone_2.entity.User;
import com.capstone.Capstone_2.entity.UserRole;
import com.capstone.Capstone_2.service.user.CustomOAuth2UserService;
import com.capstone.Capstone_2.service.user.CustomUserDetailsService;
import com.capstone.Capstone_2.service.course.LikeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
// import org.springframework.security.test.context.support.WithUserDetails; // ⬅️ 1. 이 import를 제거합니다.
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors; // ⬅️ 2. 이 import를 추가합니다.
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
// import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf; // ⬅️ (필요 없음)
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(LikeController.class)
@Import(SecurityConfig.class)
class LikeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private LikeService likeService;

    // --- SecurityConfig 의존성 MockBean (유지) ---
    @MockBean
    private CustomUserDetailsService customUserDetailsService;
    @MockBean
    private JwtUtil jwtUtil;
    @MockBean
    private CustomOAuth2UserService customOAuth2UserService;
    @MockBean
    private OAuth2SuccessHandler oAuth2SuccessHandler;
    // --- ---

    private UUID courseId;
    private UUID userId;
    private String userEmail = "testuser@example.com";

    private UserPrincipal mockPrincipal; // ⬅️ 3. mockPrincipal을 필드로 선언

    @BeforeEach
    void setUp() {
        courseId = UUID.randomUUID();
        userId = UUID.randomUUID();

        User testUser = User.builder()
                .id(userId)
                .email(userEmail)
                .role(UserRole.USER)
                .passwordHash("dummy_password_for_test") // NPE 방지용 (유지)
                .build();

        // ⬅️ 4. 모의 UserPrincipal 객체를 필드에 저장
        mockPrincipal = new UserPrincipal(testUser);

        // ⬇️ 5. given() 모킹은 @WithUserDetails를 사용하지 않으므로 이제 필요 없습니다.
        // given(customUserDetailsService.loadUserByUsername(userEmail)).willReturn(mockPrincipal);
    }

    @Test
    // @WithUserDetails(value = "testuser@example.com", userDetailsServiceBeanName = "customUserDetailsService") // ⬅️ 6. 어노테이션 제거
    @DisplayName("좋아요 토글 API - 성공 (인증된 사용자)")
    void toggleLike_success_whenAuthenticated() throws Exception {
        // Given
        LikeDto likeDto = new LikeDto(userId, courseId, true, 10);
        given(likeService.toggleLike(eq(courseId), eq(userEmail))).willReturn(likeDto);

        // When & Then
        mockMvc.perform(post("/api/courses/{courseId}/likes/toggle", courseId)
                        .with(SecurityMockMvcRequestPostProcessors.user(mockPrincipal))) // ⬅️ 7. 여기에 직접 Principal 주입
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.liked").value(true))
                .andExpect(jsonPath("$.likeCount").value(10))
                .andExpect(jsonPath("$.userId").value(userId.toString()));
    }

    @Test
    @DisplayName("좋아요 토글 API - 실패 (인증되지 않은 사용자)")
    void toggleLike_fail_whenUnauthenticated() throws Exception {
        // Given (No @WithUserDetails)

        // When & Then
        mockMvc.perform(post("/api/courses/{courseId}/likes/toggle", courseId))
                .andExpect(status().isUnauthorized()); // (이 테스트는 변경 없음)
    }
}