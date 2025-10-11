package com.capstone.Capstone_2.controller;

import com.capstone.Capstone_2.dto.SignUpDto;
import com.capstone.Capstone_2.dto.UserPrincipal;
import com.capstone.Capstone_2.entity.CreatorProfile;
import com.capstone.Capstone_2.entity.User;
import com.capstone.Capstone_2.entity.UserRole;
import com.capstone.Capstone_2.repository.UserRepository;
import com.capstone.Capstone_2.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
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
    public String registerPage() {
        return "auth/register";
    }

    @PostMapping("/register")
    public String processRegistration(@ModelAttribute SignUpDto signUpDto) {
        userService.registerNewUser(signUpDto);
        return "redirect:/auth/login";
    }

    @GetMapping("/login")
    public String loginPage() {
        return "auth/login";
    }
}