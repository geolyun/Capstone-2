package com.capstone.Capstone_2.service;

import com.capstone.Capstone_2.dto.ReportResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface AdminService {

    Page<ReportResponseDto> getReports(Pageable pageable);

    void resolveReport(UUID reportId);
}