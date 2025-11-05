package com.capstone.Capstone_2.dto;

import lombok.*;

import java.util.List;
import java.util.Map;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class RecommendationTemplateDto {
    private String code;
    private String title;
    private String summary;
    private List<TemplateSpotDto> spots; // 타입을 Map이 아닌 TemplateSpotDto로 변경
}
