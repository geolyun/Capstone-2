package com.capstone.Capstone_2.controller;

import com.capstone.Capstone_2.dto.SignUpDto;
import com.capstone.Capstone_2.dto.UserPrincipal;
import com.capstone.Capstone_2.entity.CreatorProfile;
import com.capstone.Capstone_2.entity.User;
import com.capstone.Capstone_2.entity.UserRole;
import com.capstone.Capstone_2.repository.UserRepository;
import com.capstone.Capstone_2.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final UserRepository userRepository;

    @GetMapping("/nickname")
    public String nicknamePage() {
        return "auth/nickname";
    }

    @PostMapping("/nickname")
    public String setNickname(@RequestParam String nickname, @AuthenticationPrincipal UserPrincipal principal) {
        User user = principal.getUser();

        // 닉네임 및 역할 업데이트
        user.setNickname(nickname);
        user.setRole(UserRole.USER); // GUEST -> USER

        CreatorProfile profile = user.getCreatorProfile();
        if (profile != null) {
            profile.setDisplayName(nickname);
        }

        userRepository.save(user);

        // 수동으로 인증 정보 갱신
        UserPrincipal newUserPrincipal = new UserPrincipal(user);
        Authentication newAuth = new UsernamePasswordAuthenticationToken(
                newUserPrincipal, null, newUserPrincipal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(newAuth);

        return "redirect:/home";
    }

    @GetMapping("/register")
    public String registerPage(Model model) {
        model.addAttribute("signUpDto", new SignUpDto());
        return "auth/register";
    }

    @PostMapping("/register")
    public String processRegistration(
            @Valid @ModelAttribute("signUpDto") SignUpDto signUpDto, // ✅ @Valid 추가
            BindingResult bindingResult) { // ✅ BindingResult 추가

        // ✅ 유효성 검사 실패 시, 에러 메시지와 함께 다시 회원가입 폼을 보여줌
        if (bindingResult.hasErrors()) {
            return "auth/register";
        }

        // 비밀번호 일치 확인 (서비스로 옮겨도 됨)
        if (!signUpDto.getPassword().equals(signUpDto.getPasswordConfirm())) {
            bindingResult.rejectValue("passwordConfirm", "passwordInCorrect", "비밀번호가 일치하지 않습니다.");
            return "auth/register";
        }

        try {
            userService.registerNewUser(signUpDto);
        } catch (IllegalStateException e) { // 이메일 중복 등 서비스 예외 처리
            bindingResult.reject("signupFailed", e.getMessage());
            return "auth/register";
        }

        return "redirect:/auth/login";
    }

    @GetMapping("/login")
    public String loginPage() {
        return "auth/login";
    }
}