package com.capstone.Capstone_2.controller;

import com.capstone.Capstone_2.dto.LikeDto;
import com.capstone.Capstone_2.service.LikeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails; // ✅ java.nio가 아닌, security의 UserDetails 또는 직접 만든 UserPrincipal 사용
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/likes")
@RequiredArgsConstructor
public class LikeController {
    private final LikeService service;

    @PostMapping("/toggle")
    @ResponseStatus(HttpStatus.OK)
    // ✅ 2. 메서드 시그니처 수정:
    //    - 위험한 @RequestParam UUID userId 제거
    //    - @RequestParam으로 '어떤 코스'를 좋아요할지 courseId를 받도록 추가
    public LikeDto toggle(@RequestParam UUID courseId, @AuthenticationPrincipal UserDetails principal) {

        // ✅ 3. 서비스 호출 수정: principal에서 얻은 안전한 userId와 파라미터로 받은 courseId를 전달
        // 이 부분은 CustomUserDetailsService의 구현에 따라 달라집니다.
        // UserDetails의 getUsername()이 User의 UUID를 반환하도록 구현했다면 아래 코드가 맞습니다.
        UUID userId = UUID.fromString(principal.getUsername());

        return service.toggle(userId, courseId);
    }
}