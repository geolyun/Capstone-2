package com.capstone.Capstone_2.controller;

import com.capstone.Capstone_2.dto.CourseDto.*;
import com.capstone.Capstone_2.dto.CourseSearchDto;
import com.capstone.Capstone_2.dto.RecommendationDto;
import com.capstone.Capstone_2.service.CourseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails; // ✅✅✅ 1. 올바른 UserDetails를 import 합니다. ✅✅✅
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/courses")
@RequiredArgsConstructor
public class CourseController {
    private final CourseService service;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Detail create(@Valid @RequestBody CreateReq req, @AuthenticationPrincipal UserDetails principal) {
        // ✅✅✅ 2. 서비스에는 UUID가 아닌, 안전하게 얻은 '이메일'을 전달합니다. ✅✅✅
        return service.create(req, principal.getUsername());
    }

    @PutMapping("/{id}")
    public Detail update(@PathVariable UUID id, @RequestBody UpdateReq req, @AuthenticationPrincipal UserDetails principal) {
        return service.update(id, req, principal.getUsername());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id, @AuthenticationPrincipal UserDetails principal) {
        service.delete(id, principal.getUsername());
    }

    @GetMapping("/{id}")
    public Detail get(@PathVariable UUID id) {
        return service.get(id);
    }

    public Page<CourseSummary> search(@ModelAttribute CourseSearchDto searchDto, Pageable pageable) {
        return service.search(searchDto, pageable);
    }

    @PostMapping("/{id}/submit")
    public Detail submit(@PathVariable UUID id, @AuthenticationPrincipal UserDetails principal) {
        return service.submitForReview(id, principal.getUsername());
    }


    @PostMapping("/{id}/approve")
    public Detail approve(@PathVariable UUID id) {
        return service.approve(id);
    }

    @PostMapping("/{id}/reject")
    public Detail reject(@PathVariable UUID id, @RequestParam String reason) {
        return service.reject(id, reason);
    }

    @GetMapping("/{courseId}/recommendations")
    public RecommendationDto getCourseRecommendations(@PathVariable UUID courseId) {
        return service.getCourseRecommendations(courseId);
    }
}