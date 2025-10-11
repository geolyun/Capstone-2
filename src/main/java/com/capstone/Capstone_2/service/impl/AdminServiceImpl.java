package com.capstone.Capstone_2.service.impl;

import com.capstone.Capstone_2.dto.ReportResponseDto;
import com.capstone.Capstone_2.entity.Report;
import com.capstone.Capstone_2.repository.ReportRepository;
import com.capstone.Capstone_2.service.AdminService;
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

    @Override
    @Transactional(readOnly = true)
    public Page<ReportResponseDto> getReports(Pageable pageable) {
        // 모든 신고를 최신순으로 페이징하여 가져옵니다.
        Page<Report> reportPage = reportRepository.findAll(pageable);

        // Page<Report>를 Page<ReportResponseDto>로 변환하여 반환합니다.
        return reportPage.map(ReportResponseDto::from);
    }

    @Override
    @Transactional
    public void resolveReport(UUID reportId) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new EntityNotFoundException("해당 ID의 신고를 찾을 수 없습니다: " + reportId));

        // 신고 상태를 '처리 완료'로 변경
        report.setStatus(Report.ReportStatus.RESOLVED);

        // Dirty Checking에 의해 트랜잭션 종료 시 자동으로 DB에 업데이트됩니다.
    }
}