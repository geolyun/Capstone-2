package com.capstone.Capstone_2.controller;

import com.capstone.Capstone_2.dto.LoginDto;
import com.capstone.Capstone_2.dto.SignUpDto;
import com.capstone.Capstone_2.dto.TokenDto;
import com.capstone.Capstone_2.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class ApiAuthController {

    private final AuthService authService;

    // ✅ POST /api/auth/signup 경로로 설정
    @PostMapping("/signup")
    public ResponseEntity<String> signup(@Valid @RequestBody SignUpDto signUpDto) {
        authService.signup(signUpDto);
        return ResponseEntity.ok("회원가입이 성공적으로 완료되었습니다.");
    }

    // ✅ POST /api/auth/login 경로로 설정
    @PostMapping("/login")
    public ResponseEntity<TokenDto> login(@Valid @RequestBody LoginDto loginDto) {
        TokenDto tokenDto = authService.login(loginDto);
        return ResponseEntity.ok(tokenDto);
    }
}