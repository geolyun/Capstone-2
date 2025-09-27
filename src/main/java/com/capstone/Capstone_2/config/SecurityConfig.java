package com.capstone.Capstone_2.config;

import com.capstone.Capstone_2.service.CustomOAuth2UserService;
import lombok.RequiredArgsConstructor; // ✅ @Autowired 대신 생성자 주입 방식으로 변경
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor // ✅ @Autowired 대신 생성자 주입 방식으로 변경
public class SecurityConfig {

    private final CustomOAuth2UserService customOAuth2UserService; // ✅ final 필드로 변경

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests((requests) -> requests
                        // ✅ 웹 페이지 경로와 API 경로를 모두 허용하도록 수정
                        .requestMatchers("/", "/auth/**", "/css/**", "/js/**", "/api/auth/**").permitAll()
                        .anyRequest().authenticated()
                )
                .formLogin((form) -> form
                        .loginPage("/auth/login")
                        .usernameParameter("email")
                        .defaultSuccessUrl("/home", true) // 로그인 성공 시 이동할 기본 페이지
                        .permitAll()
                )
                .logout((logout) -> logout
                        .logoutSuccessUrl("/")
                        .permitAll()
                )
                .oauth2Login(oauth2 -> oauth2
                        .loginPage("/auth/login")
                        .defaultSuccessUrl("/home", true)
                        .userInfoEndpoint(userInfo -> userInfo
                                .userService(customOAuth2UserService)
                        )
                );

        http.csrf((csrf) -> csrf.disable());
        http.headers((headers) -> headers.frameOptions((frameOptions) -> frameOptions.sameOrigin()));

        return http.build();
    }
}