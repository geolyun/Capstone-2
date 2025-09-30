package com.capstone.Capstone_2.service;

import com.capstone.Capstone_2.entity.User;
import com.capstone.Capstone_2.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true) // ✅ 조회만 하므로 readOnly = true 추가
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        // 1. 이메일로 사용자를 찾습니다. 없으면 예외를 던집니다.
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("해당 이메일을 가진 사용자를 찾을 수 없습니다: " + email));

        // 2. ✅ 사용자의 역할(role)을 Spring Security가 인식할 수 있는 권한 형태로 변환합니다.
        //    (예: "user" -> "ROLE_USER")
        //    이 부분이 없으면 인증에 실패할 수 있습니다.
        List<GrantedAuthority> authorities = Collections.singletonList(
                new SimpleGrantedAuthority("ROLE_" + user.getRole().toUpperCase())
        );

        // 3. ✅ Spring Security의 UserDetails 객체를 생성하여 반환합니다.
        //    이 객체에는 '로그인에 사용할 ID(이메일)', 'DB에 저장된 암호화된 비밀번호', '권한 목록'이
        //    반드시 정확하게 담겨야 합니다.
        return new org.springframework.security.core.userdetails.User(
                user.getEmail(),
                user.getPasswordHash(),
                authorities
        );
    }
}