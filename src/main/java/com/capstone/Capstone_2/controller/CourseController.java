package com.capstone.Capstone_2.controller;

import com.capstone.Capstone_2.dto.CourseDto.*;
import com.capstone.Capstone_2.service.CourseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.nio.file.attribute.UserPrincipal;
import java.util.UUID;


@RestController @RequestMapping("/api/courses") @RequiredArgsConstructor
public class CourseController {
    private final CourseService service;


    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Detail create(@Valid @RequestBody CreateReq req, @AuthenticationPrincipal UserDetails principal) {
        return service.create(req, UUID.fromString(principal.getUsername())); // 예시: principal에 UUID id가 있다고 가정
    }


    @PutMapping("/{id}")
    public Detail update(@PathVariable UUID id, @RequestBody UpdateReq req, @AuthenticationPrincipal UserDetails principal) {
        return service.update(id, req, UUID.fromString(principal.getUsername())); // 예시
    }


    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id, @AuthenticationPrincipal UserDetails principal) {
        service.delete(id, UUID.fromString(principal.getUsername())); // 예시
    }


    @GetMapping("/{id}")
    public Detail get(@PathVariable UUID id) {
        return service.get(id);
    }


    @GetMapping
    public Page<CourseSummary> search(@RequestParam(required = false) String q, Pageable pageable) {
        return service.search(q, pageable);
    }


    @PostMapping("/{id}/submit")
    public Detail submit(@PathVariable UUID id, @AuthenticationPrincipal UserDetails principal) {
        return service.submitForReview(id, UUID.fromString(principal.getUsername()));
    }

    @PostMapping("/{id}/approve")
    public Detail approve(@PathVariable UUID id) {
        return service.approve(id);
    }

    @PostMapping("/{id}/reject")
    public Detail reject(@PathVariable UUID id, @RequestParam String reason) {
        return service.reject(id, reason);
    }
}