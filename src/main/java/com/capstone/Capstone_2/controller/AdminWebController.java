package com.capstone.Capstone_2.controller;

import com.capstone.Capstone_2.dto.AdminDashboardDto;
import com.capstone.Capstone_2.dto.ReportResponseDto;
import com.capstone.Capstone_2.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminWebController {

    private final AdminService adminService;

    // ✅ 대시보드 메인 페이지
    @GetMapping
    public String dashboard(Model model) {
        AdminDashboardDto stats = adminService.getDashboardStats();
        model.addAttribute("stats", stats);
        return "admin/dashboard"; // templates/admin/dashboard.html 뷰
    }

    @GetMapping("/reports")
    public String reportDashboard(Model model, Pageable pageable) {
        Page<ReportResponseDto> reportPage = adminService.getReports(pageable);
        model.addAttribute("reportPage", reportPage);
        return "admin/reports";
    }
}