package com.capstone.Capstone_2.config;

import com.capstone.Capstone_2.service.CustomUserDetailsService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final CustomUserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");
        String jwt = null;
        String userEmail = null;

        // 1. 헤더 확인
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            jwt = authHeader.substring(7);
            try {
                // ✅ 2. [수정] 토큰 파싱 시도 (에러 발생 가능 구간)
                userEmail = jwtUtil.extractUsername(jwt);
            } catch (Exception e) {
                // 🚨 토큰이 만료되었거나 잘못된 경우
                // 로그만 남기고 인증 절차를 건너뜁니다. (401 에러를 내지 않음)
                logger.warn("JWT Token error: " + e.getMessage());
            }
        }

        // 3. 인증 진행 (이메일이 정상적으로 추출된 경우에만)
        if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                UserDetails userDetails = this.userDetailsService.loadUserByUsername(userEmail);

                if (jwtUtil.isTokenValid(jwt, userDetails)) {
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities()
                    );
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            } catch (Exception e) {
                // 사용자 조회 실패 등의 에러도 무시하고 진행
                logger.warn("User authentication failed: " + e.getMessage());
            }
        }

        // 4. 다음 필터로 진행 (로그인이 안 된 상태라도 요청을 허용)
        filterChain.doFilter(request, response);
    }
}