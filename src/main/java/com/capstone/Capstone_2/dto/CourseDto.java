package com.capstone.Capstone_2.dto;

import com.capstone.Capstone_2.entity.ReviewState;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.*;

public class CourseDto {

    @Getter
    @Setter
    @NoArgsConstructor
    public static class SpotReq {
        @NotNull private Integer orderNo;
        @NotBlank private String title;
        private String description;
        private BigDecimal lat;
        private BigDecimal lng;
        private List<String> images = new ArrayList<>(); // 초기화 추가
        private String imagesInput;
        private Integer stayMinutes;
        private Integer price;
    }


    @Getter
    @Setter
    @NoArgsConstructor
    public static class CreateReq {
        private UUID categoryId;
        @NotBlank @Size(max = 100)
        private String title;
        private String summary;
        private String coverImageUrl;
        private String regionCode;
        private String regionName;
        private Integer durationMinutes;
        private Integer estimatedCost;

        // API용 + 웹 파싱 후 저장용
        private List<String> tags = new ArrayList<>();

        // 웹 폼 입력용 임시 필드
        private String tagsString;

        // private Map<String, Object> metadata = new HashMap<>();
        // private String metadataJson;

        @NotNull
        private List<SpotReq> spots = new ArrayList<>(); // 초기화 추가
    }


    @Getter
    @Setter
    @NoArgsConstructor
    public static class UpdateReq {
        private UUID categoryId;
        private String title;
        private String summary;
        private String coverImageUrl;
        private String regionCode;
        private String regionName;
        private Integer durationMinutes;
        private Integer estimatedCost;
        private List<String> tags;
        private String tagsString; // 웹 폼용
        // private Map<String, Object> metadata;
        // private String metadataJson;
        private List<SpotReq> spots;
    }


    public record CourseSummary(
            UUID id,
            String title,
            String summary,
            String coverImageUrl,
            String regionName,
            Integer durationMinutes,
            Integer estimatedCost,
            Integer likeCount,
            Integer purchaseCount,
            ReviewState reviewState,
            java.time.LocalDateTime createdAt,
            BigDecimal lat,
            BigDecimal lng
    ) {}


    public record SpotRes(
            Integer orderNo,
            String title,
            String description,
            BigDecimal lat,
            BigDecimal lng,
            List<String> images,
            Integer stayMinutes,
            Integer price
    ) {}

    public record Detail(
            java.util.UUID id,
            java.util.UUID creatorId,
            String creatorDisplayName,
            String categorySlug,
            String title,
            String summary,
            String coverImageUrl,
            String regionCode,
            String regionName,
            Integer durationMinutes,
            Integer estimatedCost,
            List<String> tags,
            // Map<String, Object> metadata,
            Integer likeCount,
            Integer purchaseCount,
            ReviewState reviewState,
            OffsetDateTime publishedAt,
            List<SpotRes> spots,
            boolean isCurrentUserLiked,
            java.time.LocalDateTime createdAt
    ) {}
}