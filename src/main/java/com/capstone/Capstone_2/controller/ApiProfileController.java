package com.capstone.Capstone_2.controller;

import com.capstone.Capstone_2.dto.CourseDto;
import com.capstone.Capstone_2.dto.CreatorProfileDto;
import com.capstone.Capstone_2.dto.ProfileDto;
import com.capstone.Capstone_2.dto.UserPrincipal;
import com.capstone.Capstone_2.service.CourseService;
import com.capstone.Capstone_2.service.CreatorProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
public class ApiProfileController {

    private final CreatorProfileService creatorProfileService;
    private final CourseService courseService;

    @GetMapping("/me")
    public ResponseEntity<ProfileDto> getMyProfile(@AuthenticationPrincipal UserPrincipal principal) {

        String email = principal.getUsername();

        ProfileDto profileDto = creatorProfileService.getProfile(email);

        return ResponseEntity.ok(profileDto);
    }

    @PutMapping("/me")
    public ResponseEntity<String> updateProfile(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody CreatorProfileDto.UpdateRequest updateRequest) {

        creatorProfileService.updateProfile(principal.getUsername(), updateRequest);
        return ResponseEntity.ok("프로필이 성공적으로 수정되었습니다.");
    }

    @GetMapping("/me/courses")
    public ResponseEntity<Page<CourseDto.CourseSummary>> getMyCourses(
            @AuthenticationPrincipal UserPrincipal principal,
            @PageableDefault(size = 5, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        return ResponseEntity.ok(courseService.getMyCourses(principal.getUsername(), pageable));
    }

    @GetMapping("/me/liked-courses")
    public ResponseEntity<Page<CourseDto.CourseSummary>> getLikedCourses(
            @AuthenticationPrincipal UserPrincipal principal,
            @PageableDefault(size = 5, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        return ResponseEntity.ok(courseService.getLikedCourses(principal.getUsername(), pageable));
    }
}