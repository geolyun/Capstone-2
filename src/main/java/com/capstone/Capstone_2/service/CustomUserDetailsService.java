package com.capstone.Capstone_2.service;

import com.capstone.Capstone_2.entity.User;
import com.capstone.Capstone_2.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority; // ✅ import 추가
import org.springframework.security.core.authority.SimpleGrantedAuthority; // ✅ import 추가
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections; // ✅ import 추가
import java.util.List; // ✅ import 추가

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("해당 이메일을 가진 사용자를 찾을 수 없습니다: " + email));

        // ✅ 사용자의 역할(예: "user", "admin")을 GrantedAuthority 객체로 변환합니다.
        List<GrantedAuthority> authorities = Collections.singletonList(
                new SimpleGrantedAuthority("ROLE_" + user.getRole().toUpperCase())
        );

        // ✅ 생성자의 마지막 인자로 변환된 권한 목록을 전달합니다.
        return new org.springframework.security.core.userdetails.User(
                user.getEmail(),
                user.getPasswordHash(),
                authorities
        );
    }
}