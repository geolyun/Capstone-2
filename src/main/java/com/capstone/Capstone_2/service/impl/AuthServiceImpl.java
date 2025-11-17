package com.capstone.Capstone_2.service.impl;

import com.capstone.Capstone_2.dto.LoginDto;
import com.capstone.Capstone_2.dto.SignUpDto;
import com.capstone.Capstone_2.dto.TokenDto;
import com.capstone.Capstone_2.service.AuthService;
import com.capstone.Capstone_2.service.UserService; // ✅ UserService 추가
import com.capstone.Capstone_2.config.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    // 기존의 userRepository, passwordEncoder 제거 (UserService가 대신함)
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final UserService userService; // ✅ UserService 주입

    @Override
    @Transactional
    public void signup(SignUpDto signUpDto) {
        // ✅ 기존 중복 로직을 모두 제거하고, 이메일 발송 기능이 있는 UserService를 호출합니다.
        userService.registerNewUser(signUpDto);
    }

    @Override
    @Transactional(readOnly = true)
    public TokenDto login(LoginDto loginDto) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginDto.getEmail(), loginDto.getPassword())
        );

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();


        String token = jwtUtil.generateToken(userDetails);
        return new TokenDto(token);
    }
}