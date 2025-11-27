package com.capstone.Capstone_2.config;

import com.capstone.Capstone_2.service.CustomOAuth2UserService;
import com.capstone.Capstone_2.service.CustomUserDetailsService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.Collections;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final CustomUserDetailsService customUserDetailsService;
    private final CustomOAuth2UserService customOAuth2UserService;
    private final OAuth2SuccessHandler oAuth2SuccessHandler;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration cfg) throws Exception {
        return cfg.getAuthenticationManager();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(customUserDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    /**
     * ✅ CORS 설정을 위한 Bean 정의
     * 프론트엔드(ringco.my)와 로컬 개발 환경(localhost:3000)에서의 접근을 허용합니다.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // 1. 허용할 프론트엔드 도메인 목록
        configuration.setAllowedOrigins(Arrays.asList(
                "https://ringco.my",
                "http://localhost:3000",
                "https://www.ringco.my",
                "https://localhost:3000"
        ));

        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));

        configuration.setAllowedHeaders(Collections.singletonList("*"));

        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    @Order(1)
    public SecurityFilterChain apiSecurityFilterChain(HttpSecurity http) throws Exception {
        System.out.println("### Initializing API Security Filter Chain ###");
        http
                // ✅ 위에서 만든 CORS 설정 Bean을 적용
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .securityMatcher("/api/**")
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .exceptionHandling(e -> e
                        .authenticationEntryPoint((req, res, ex) -> res.setStatus(HttpServletResponse.SC_UNAUTHORIZED))
                        .accessDeniedHandler((req, res, ex) -> res.setStatus(HttpServletResponse.SC_FORBIDDEN))
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST, "/api/auth/signup", "/api/auth/login", "/api/auth/verify").permitAll()

                        .requestMatchers(HttpMethod.GET, "/api/courses/**", "/api/categories/**").permitAll()

                        .requestMatchers(HttpMethod.POST, "/api/files/upload").authenticated()

                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    @Order(2)
    public SecurityFilterChain webSecurityFilterChain(HttpSecurity http) throws Exception {
        System.out.println("### Initializing WEB Security Filter Chain ###");
        http
                // ✅ 위에서 만든 CORS 설정 Bean을 적용
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .authenticationProvider(authenticationProvider())
                .csrf(csrf -> csrf.ignoringRequestMatchers(AntPathRequestMatcher.antMatcher("/h2-console/**")))
                .headers(headers -> headers.frameOptions(frameOptions -> frameOptions.sameOrigin()))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .requestMatchers("/error").permitAll()
                        .requestMatchers("/", "/auth/**", "/css/**", "/js/**", "/h2-console/**", "/privacy", "/terms", "/legal/terms-content", "/legal/privacy-content").permitAll()
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/auth/login")
                        .usernameParameter("email")
                        .defaultSuccessUrl("/home", true)
                        .permitAll()
                )
                .logout(l -> l.logoutSuccessUrl("/").permitAll())
                .oauth2Login(oauth2 -> oauth2
                        .loginPage("/auth/login")
                        .successHandler(oAuth2SuccessHandler)
                        .userInfoEndpoint(userInfo -> userInfo.userService(customOAuth2UserService))
                );

        return http.build();
    }
}