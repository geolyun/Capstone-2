package com.capstone.Capstone_2.dto;

import com.capstone.Capstone_2.entity.Report;
import lombok.Builder;
import lombok.Getter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Builder
public class ReportResponseDto {
    private UUID reportId;
    private UUID reportedCourseId;
    private String reportedCourseTitle;
    private UUID reporterId;
    private String reporterNickname;
    private String reason;
    private String description;
    private String status;
    private OffsetDateTime createdAt;

    public static ReportResponseDto from(Report report) {
        return ReportResponseDto.builder()
                .reportId(report.getId())
                .reportedCourseId(report.getReportedCourse().getId())
                .reportedCourseTitle(report.getReportedCourse().getTitle())
                .reporterId(report.getReporter().getId())
                .reporterNickname(report.getReporter().getNickname())
                .reason(report.getReason().name())
                .description(report.getDescription())
                .status(report.getStatus().name())
                .createdAt(report.getCreatedAt())
                .build();
    }
}