package com.capstone.Capstone_2.controller;

import com.capstone.Capstone_2.dto.LoginDto;
import com.capstone.Capstone_2.dto.SignUpDto;
import com.capstone.Capstone_2.dto.TokenDto;
import com.capstone.Capstone_2.service.AuthService;
import com.capstone.Capstone_2.service.UserService;
import jakarta.validation.Valid;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
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
    private final UserService userService;

    // ✅ POST /api/auth/signup 경로로 설정
    @PostMapping("/signup")
    public ResponseEntity<String> signup(@Valid @RequestBody SignUpDto signUpDto) {
        authService.signup(signUpDto);
        return ResponseEntity.ok("회원가입 요청 성공. 이메일로 전송된 인증 코드를 확인해주세요.");
    }

    // 로그인
    @PostMapping("/login")
    public ResponseEntity<TokenDto> login(@Valid @RequestBody LoginDto loginDto) {
        TokenDto tokenDto = authService.login(loginDto);
        return ResponseEntity.ok(tokenDto);
    }

    // ✅ [신규] 이메일 인증 코드 확인 API
    @PostMapping("/verify")
    public ResponseEntity<String> verifyEmail(@RequestBody VerificationRequest request) {
        boolean isVerified = userService.verifyCode(request.getEmail(), request.getCode());

        if (isVerified) {
            return ResponseEntity.ok("이메일 인증이 성공적으로 완료되었습니다.");
        } else {
            return ResponseEntity.badRequest().body("인증 실패: 코드가 잘못되었거나 만료되었습니다.");
        }
    }

    // ✅ 인증 요청용 DTO (내부 클래스)
    @Getter
    @Setter
    public static class VerificationRequest {
        private String email;
        private String code;
    }
}