package com.capstone.Capstone_2.dto;

import com.capstone.Capstone_2.dto.CourseDto.CourseSummary;
import lombok.Builder;
import lombok.Getter;
import java.util.List;

@Getter
@Builder
public class RecommendationDto {
    private List<CourseSummary> relatedByLikes;
    private List<CourseSummary> sameCategory;
    private List<CourseSummary> sameRegion;
}