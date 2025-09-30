package com.capstone.Capstone_2.config;


import com.capstone.Capstone_2.service.CustomUserDetailsService;
// import com.capstone.Capstone_2.util.JwtUtil; // 직접 만드신 JwtUtil 클래스의 경로
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

@Component // Spring이 이 필터를 자동으로 인식하도록 Bean으로 등록
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil; // JWT 토큰을 검증하고 정보를 추출하는 유틸리티
    private final CustomUserDetailsService userDetailsService; // 사용자 정보를 DB에서 가져오는 서비스

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        // 1. 요청 헤더에서 'Authorization' 값을 가져옵니다.
        final String authHeader = request.getHeader("Authorization");

        // 2. Authorization 헤더가 없거나 'Bearer '로 시작하지 않으면,
        //    이것은 JWT 인증이 필요한 요청이 아니므로 다음 필터로 넘어갑니다.
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        // 3. 'Bearer ' 부분을 제외한 실제 JWT 토큰만 추출합니다.
        final String jwt = authHeader.substring(7);

        // 4. JWT 토큰에서 사용자의 이메일(username)을 추출합니다.
        final String userEmail = jwtUtil.extractUsername(jwt);

        // 5. 이메일이 존재하고, 아직 현재 스레드의 SecurityContext에 인증 정보가 없는 경우에만 인증 절차를 진행합니다.
        //    (이미 인증된 요청을 다시 처리할 필요가 없기 때문)
        if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {

            // 6. 추출한 이메일을 사용하여 데이터베이스에서 사용자 정보를 조회합니다.
            UserDetails userDetails = this.userDetailsService.loadUserByUsername(userEmail);

            // 7. 조회된 사용자 정보와 JWT 토큰이 유효한지 최종 확인합니다.
            if (jwtUtil.isTokenValid(jwt, userDetails)) {

                // 8. 토큰이 유효하면, Spring Security가 사용할 인증 토큰(AuthenticationToken)을 생성합니다.
                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null, // JWT 방식에서는 비밀번호를 사용하지 않으므로 null
                        userDetails.getAuthorities()
                );

                // 9. 인증 토큰에 현재 요청에 대한 세부 정보를 추가합니다.
                authToken.setDetails(
                        new WebAuthenticationDetailsSource().buildDetails(request)
                );

                // 10. SecurityContextHolder에 인증 정보를 등록합니다.
                //     이 시점부터 현재 요청에서는 사용자가 '인증된' 상태가 됩니다.
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }

        // 11. 다음 필터로 요청과 응답을 전달합니다.
        filterChain.doFilter(request, response);
    }
}