package com.capstone.Capstone_2.controller;

import com.capstone.Capstone_2.dto.CourseDto.*;
import com.capstone.Capstone_2.dto.CourseSearchDto;
import com.capstone.Capstone_2.dto.RecommendationDto;
import com.capstone.Capstone_2.service.course.CourseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/courses")
@RequiredArgsConstructor
public class CourseController {

    private final CourseService service;

    @PostMapping
    public ResponseEntity<Detail> create(@Valid @RequestBody CreateReq req, @AuthenticationPrincipal UserDetails principal) {
        return ResponseEntity.status(201).body(service.create(req, principal.getUsername()));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Detail> update(@PathVariable UUID id, @RequestBody UpdateReq req, @AuthenticationPrincipal UserDetails principal) {
        return ResponseEntity.ok(service.update(id, req, principal.getUsername()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id, @AuthenticationPrincipal UserDetails principal) {
        service.delete(id, principal.getUsername());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Detail> get(@PathVariable UUID id, @AuthenticationPrincipal UserDetails principal) {
        String email = (principal != null) ? principal.getUsername() : null;
        return ResponseEntity.ok(service.get(id, email));
    }

    @GetMapping
    public ResponseEntity<Page<CourseSummary>> search(@ModelAttribute CourseSearchDto searchDto, Pageable pageable) {
        return ResponseEntity.ok(service.search(searchDto, pageable));
    }

    @PostMapping("/{id}/submit")
    public ResponseEntity<Detail> submit(@PathVariable UUID id, @AuthenticationPrincipal UserDetails principal) {
        return ResponseEntity.ok(service.submitForReview(id, principal.getUsername()));
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<Detail> approve(@PathVariable UUID id) {
        return ResponseEntity.ok(service.approve(id));
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<Detail> reject(@PathVariable UUID id, @RequestParam String reason) {
        return ResponseEntity.ok(service.reject(id, reason));
    }

    @GetMapping("/{courseId}/recommendations")
    public ResponseEntity<RecommendationDto> getCourseRecommendations(@PathVariable UUID courseId) {
        return ResponseEntity.ok(service.getCourseRecommendations(courseId));
    }
}