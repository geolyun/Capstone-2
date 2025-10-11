package com.capstone.Capstone_2.controller;

import com.capstone.Capstone_2.dto.CreatorProfileDto;
import com.capstone.Capstone_2.dto.ProfileDto;
import com.capstone.Capstone_2.dto.UserPrincipal;
import com.capstone.Capstone_2.service.CreatorProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final CreatorProfileService creatorProfileService;

    @GetMapping("/me")
    public String myProfilePage(@AuthenticationPrincipal UserPrincipal principal, Model model) {
        ProfileDto profileDto = ProfileDto.from(principal.getUser());
        model.addAttribute("profile", profileDto);
        return "profile/my-profile";
    }

    @GetMapping("/me/edit")
    public String editProfilePage(@AuthenticationPrincipal UserPrincipal principal, Model model) {
        // 현재 프로필 정보를 DTO에 담아 폼에 기본값으로 채워줍니다.
        ProfileDto profileDto = ProfileDto.from(principal.getUser());
        model.addAttribute("profile", profileDto);
        return "profile/profile-edit";
    }

    // ✅ 2. 프로필 수정 폼 제출을 처리하는 메서드 추가
    @PostMapping("/me/edit")
    public String updateProfile(
            @AuthenticationPrincipal UserPrincipal principal,
            @ModelAttribute("profile") CreatorProfileDto.UpdateRequest updateRequest) {

        creatorProfileService.updateProfile(principal.getUsername(), updateRequest);

        // 수정 완료 후, '내 프로필' 페이지로 다시 이동
        return "redirect:/profile/me";
    }
}