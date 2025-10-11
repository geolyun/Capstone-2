package com.capstone.Capstone_2.controller;

import com.capstone.Capstone_2.entity.Report;
import com.capstone.Capstone_2.repository.ReportRepository;
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

    private final ReportRepository reportRepository; // 간단한 조회를 위해 직접 사용

    @GetMapping("/reports")
    public String reportDashboard(Model model, Pageable pageable) {
        Page<Report> reportPage = reportRepository.findByStatus(Report.ReportStatus.PENDING, pageable);
        model.addAttribute("reportPage", reportPage);
        return "admin/reports"; // templates/admin/reports.html 뷰
    }
}