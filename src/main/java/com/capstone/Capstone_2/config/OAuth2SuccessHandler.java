package com.capstone.Capstone_2.config;

import com.capstone.Capstone_2.dto.UserPrincipal;
import com.capstone.Capstone_2.entity.User;
import com.capstone.Capstone_2.entity.UserRole;
import com.capstone.Capstone_2.repository.UserRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication)
            throws IOException, ServletException {

        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        User user = principal.getUser();

        String targetPath;

        if (user.getRole() == UserRole.GUEST) {
            String randomNickname = "User_" + UUID.randomUUID().toString().substring(0, 8);

            user.setNickname(randomNickname);
            user.setRole(UserRole.USER);

            if (user.getCreatorProfile() != null) {
                user.getCreatorProfile().setDisplayName(randomNickname);
            }

            userRepository.save(user);

            principal = new UserPrincipal(user, principal.getAttributes());
            Authentication newAuth = new UsernamePasswordAuthenticationToken(
                    principal, null, principal.getAuthorities());
            SecurityContextHolder.getContext().setAuthentication(newAuth);

            targetPath = "/profile";
        } else {
            targetPath = "/";
        }

        String token = jwtUtil.generateToken(principal);

        String targetUrl = UriComponentsBuilder.fromUriString("https://ringco.my" + targetPath)
                .queryParam("token", token)
                .build().toUriString();

        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
}