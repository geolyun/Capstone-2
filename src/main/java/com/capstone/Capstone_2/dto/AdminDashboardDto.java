package com.capstone.Capstone_2.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AdminDashboardDto {
    private long totalUsers;
    private long totalCourses;
    private long totalLikes;
    private long pendingReports;
}

