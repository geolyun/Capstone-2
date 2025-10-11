package com.capstone.Capstone_2.controller;

import com.capstone.Capstone_2.dto.ReportResponseDto;
import com.capstone.Capstone_2.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')") // ADMIN 역할만 접근 가능
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    // 신고 목록 조회 API
    @GetMapping("/reports")
    public Page<ReportResponseDto> getReports(Pageable pageable) {
        return adminService.getReports(pageable);
    }

    // 신고 처리 API
    @PostMapping("/reports/{reportId}/resolve")
    public ResponseEntity<Void> resolveReport(@PathVariable UUID reportId) {
        adminService.resolveReport(reportId);
        return ResponseEntity.ok().build();
    }
}
