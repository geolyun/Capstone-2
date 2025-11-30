package com.capstone.Capstone_2.controller;

import com.capstone.Capstone_2.dto.LikeDto;
import com.capstone.Capstone_2.dto.UserPrincipal;
import com.capstone.Capstone_2.service.LikeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/courses/{courseId}/likes")
@RequiredArgsConstructor
public class LikeController {

    private final LikeService likeService;

    @PostMapping("/toggle")
    public ResponseEntity<LikeDto> toggleLike(
            @PathVariable UUID courseId,
            @AuthenticationPrincipal UserPrincipal principal) {

        LikeDto result = likeService.toggleLike(courseId, principal.getUsername());

        return ResponseEntity.ok(result);
    }
}