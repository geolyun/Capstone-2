package com.capstone.Capstone_2.dto;

import com.capstone.Capstone_2.entity.Report.ReportReason;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

public class ReportDto {
    @Getter
    @Setter
    public static class CreateRequest {
        @NotNull(message = "신고 사유는 필수입니다.")
        private ReportReason reason;

        @NotBlank(message = "상세 설명은 필수입니다.")
        private String description;
    }
}