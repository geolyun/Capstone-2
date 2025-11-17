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
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

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
            @Valid @ModelAttribute("signUpDto") SignUpDto signUpDto,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            return "auth/register";
        }

        if (!signUpDto.getPassword().equals(signUpDto.getPasswordConfirm())) {
            bindingResult.rejectValue("passwordConfirm", "passwordInCorrect", "비밀번호가 일치하지 않습니다.");
            return "auth/register";
        }

        try {
            userService.registerNewUser(signUpDto);
        } catch (IllegalStateException e) {
            bindingResult.reject("signupFailed", e.getMessage());
            return "auth/register";
        }

        redirectAttributes.addAttribute("email", signUpDto.getEmail());
        return "redirect:/auth/verify";
    }

    @GetMapping("/login")
    public String loginPage(@ModelAttribute("message") String message,
                            @ModelAttribute("error") String error,
                            Model model) {
        model.addAttribute("message", message);
        model.addAttribute("error", error);
        return "auth/login";
    }

    /**
     * 인증 코드 입력 폼을 보여주는 GET 엔드포인트
     */
    @GetMapping("/verify")
    public String verifyCodeForm(@RequestParam("email") String email,
                                 @ModelAttribute("error") String error, // POST에서 실패 시 전달되는 에러
                                 Model model) {
        model.addAttribute("email", email);
        model.addAttribute("error", error);
        return "auth/verify-form"; // ✅ templates/auth/verify-form.html
    }

    /**
     * 인증 코드를 검증하는 POST 엔드포인트
     */
    @PostMapping("/verify")
    public String verifyCode(@RequestParam("email") String email,
                             @RequestParam("code") String code,
                             RedirectAttributes redirectAttributes) {

        boolean success = userService.verifyCode(email, code);

        if (success) {
            // ✅ 인증 성공: 로그인 페이지로 리다이렉트하며 성공 메시지 전달
            redirectAttributes.addFlashAttribute("message", "이메일 인증이 완료되었습니다! 로그인해주세요.");
            return "redirect:/auth/login";
        } else {
            // ❌ 인증 실패: 다시 인증 폼으로 리다이렉트하며 에러 메시지 전달
            redirectAttributes.addFlashAttribute("error", "인증 번호가 올바르지 않거나 만료되었습니다.");
            redirectAttributes.addAttribute("email", email); // 쿼리 파라미터 유지를 위해
            return "redirect:/auth/verify";
        }
    }
}