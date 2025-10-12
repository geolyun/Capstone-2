package com.capstone.Capstone_2.config;

import com.capstone.Capstone_2.entity.UserRole;
import com.capstone.Capstone_2.dto.UserPrincipal;
import com.capstone.Capstone_2.config.JwtUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder; // ✅ UriComponentsBuilder import 추가

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtUtil jwtUtil;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication)
            throws IOException, ServletException {

        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();

        if (principal.getUser().getRole() == UserRole.GUEST) {
            getRedirectStrategy().sendRedirect(request, response, "/auth/nickname");

        } else {

            String token = jwtUtil.generateToken(principal);

            // 프론트엔드 주소는 실제 프로젝트에 맞게 변경해야 함. (예: React, Vue 앱 주소)
            String targetUrl = UriComponentsBuilder.fromUriString("http://localhost:3000/oauth-redirect")
                    .queryParam("token", token)
                    .build().toUriString();


            getRedirectStrategy().sendRedirect(request, response, targetUrl);
        }
    }
}