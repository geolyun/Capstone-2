package com.capstone.Capstone_2.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class CourseSearchDto {
    private String q; // 텍스트 검색어
    private String region;
    private UUID categoryId;
    private Integer maxCost;
    private Integer maxDuration;
    private String tag;
    private String sortType;
}