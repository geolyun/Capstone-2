package com.capstone.Capstone_2.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class CourseSearchDto {
    private String q; // 텍스트 검색어 (제목, 요약)
    private String region; // 지역 코드
    private UUID categoryId;
    private Integer maxCost; // 최대 예산
    private Integer maxDuration; // 최대 소요 시간 (분)
    private String tag; // 단일 태그 검색
    private String sortType;
}