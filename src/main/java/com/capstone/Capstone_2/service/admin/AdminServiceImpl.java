package com.capstone.Capstone_2.service.admin;

import com.capstone.Capstone_2.dto.AdminDashboardDto;
import com.capstone.Capstone_2.dto.ReportResponseDto;
import com.capstone.Capstone_2.entity.Report;
import com.capstone.Capstone_2.entity.ReportStatus;
import com.capstone.Capstone_2.repository.CourseRepository;
import com.capstone.Capstone_2.repository.LikeRepository;
import com.capstone.Capstone_2.repository.ReportRepository;
import com.capstone.Capstone_2.repository.UserRepository;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {

    private final ReportRepository reportRepository;
    private final UserRepository userRepository;
    private final CourseRepository courseRepository;
    private final LikeRepository likeRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<ReportResponseDto> getReports(Pageable pageable) {
        // 모든 신고를 최신순으로 페이징
        Page<Report> reportPage = reportRepository.findAll(pageable);

        return reportPage.map(ReportResponseDto::from);
    }

    @Override
    @Transactional
    public void resolveReport(UUID reportId) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new EntityNotFoundException("해당 ID의 신고를 찾을 수 없습니다: " + reportId));

        // 신고 상태를 '처리 완료'로 변경
        report.setStatus(ReportStatus.RESOLVED);
    }

    @Override
    @Transactional(readOnly = true)
    public AdminDashboardDto getDashboardStats() {
        long totalUsers = userRepository.count();
        long totalCourses = courseRepository.count();
        long totalLikes = likeRepository.count();
        long pendingReports = reportRepository.countByStatus(ReportStatus.PENDING);

        return AdminDashboardDto.builder()
                .totalUsers(totalUsers)
                .totalCourses(totalCourses)
                .totalLikes(totalLikes)
                .pendingReports(pendingReports)
                .build();
    }
}