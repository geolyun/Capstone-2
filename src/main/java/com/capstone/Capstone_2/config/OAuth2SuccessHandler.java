package com.capstone.Capstone_2.config;

import com.capstone.Capstone_2.entity.UserRole;
import com.capstone.Capstone_2.dto.UserPrincipal;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication)
            throws IOException, ServletException {

        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();

        // 사용자의 역할(Role)을 확인합니다.
        if (principal.getUser().getRole() == UserRole.GUEST) {
            // 역할이 GUEST이면, 닉네임 설정 페이지로 리디렉션합니다.
            getRedirectStrategy().sendRedirect(request, response, "/auth/nickname");
        } else {
            // 역할이 GUEST가 아니면 (기존 USER), 홈으로 리디렉션합니다.
            getRedirectStrategy().sendRedirect(request, response, "/home");
        }
    }
}