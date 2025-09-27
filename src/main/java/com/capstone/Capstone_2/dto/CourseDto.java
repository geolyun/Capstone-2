package com.capstone.Capstone_2.dto;

import com.capstone.Capstone_2.entity.ReviewState;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class CourseDto {
    public record SpotReq(
            @NotNull Integer orderNo,
            @NotBlank String title,
            String description,
            BigDecimal lat,
            BigDecimal lng,
            List<String> images,
            Integer stayMinutes,
            Integer price
    ) {}


    public record CreateReq(
            @NotNull UUID creatorId,
            UUID categoryId,
            @NotBlank @Size(max = 100) String title,
            String summary,
            String coverImageUrl,
            String regionCode,
            String regionName,
            Integer durationMinutes,
            Integer estimatedCost,
            List<String> tags,
            Map<String, Object> metadata,
            @NotNull List<SpotReq> spots
    ) {}


    public record UpdateReq(
            UUID categoryId,
            String title,
            String summary,
            String coverImageUrl,
            String regionCode,
            String regionName,
            Integer durationMinutes,
            Integer estimatedCost,
            List<String> tags,
            Map<String, Object> metadata,
            List<SpotReq> spots
    ) {}


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
            ReviewState reviewState
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
            Map<String, Object> metadata,
            Integer likeCount,
            Integer purchaseCount,
            ReviewState reviewState,
            OffsetDateTime publishedAt,
            List<SpotRes> spots
    ) {}
}