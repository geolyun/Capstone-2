package com.capstone.Capstone_2.controller;

// ✅ 1. UserDto 대신 SignUpDto를 import 합니다.
import com.capstone.Capstone_2.dto.SignUpDto;
import com.capstone.Capstone_2.service.UserService;
import lombok.RequiredArgsConstructor; // ✅ @Autowired 대신 최신 방식인 생성자 주입을 사용하는 것이 좋습니다.
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/auth") // '/auth' 경로의 요청을 이 컨트롤러가 처리하도록 지정
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    // 1. 로그인 페이지를 보여주는 메서드
    @GetMapping("/login")
    public String loginPage() {
        return "auth/login"; // templates/auth/login.html 렌더링
    }

    // 2. 회원가입 페이지를 보여주는 메서드
    @GetMapping("/register")
    public String registerPage() {
        return "auth/register"; // templates/auth/register.html 렌더링
    }

    // 3. ✅ 회원가입 폼 데이터를 '처리'하는 메서드
    @PostMapping("/register")
    public String processRegistration(@ModelAttribute SignUpDto signUpDto) {
        userService.registerNewUser(signUpDto);
        // 회원가입 성공 시, 로그인 페이지로 리디렉션
        return "redirect:/auth/login";
    }
}