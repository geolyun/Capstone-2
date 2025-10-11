package com.capstone.Capstone_2.controller;

import com.capstone.Capstone_2.dto.ReportDto;
import com.capstone.Capstone_2.dto.UserPrincipal;
import com.capstone.Capstone_2.service.ReportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController
@RequestMapping("/api/courses/{courseId}/report")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @PostMapping
    public ResponseEntity<String> reportCourse(
            @PathVariable UUID courseId,
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody ReportDto.CreateRequest request) {

        reportService.createReport(courseId, principal.getUsername(), request);
        return ResponseEntity.ok("신고가 성공적으로 접수되었습니다.");
    }
}