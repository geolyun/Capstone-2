package com.capstone.Capstone_2.service.course;

import com.capstone.Capstone_2.dto.ReportDto;

import java.util.UUID;

public interface ReportService {
    void createReport(UUID courseId, String reporterEmail, ReportDto.CreateRequest request);
}
