package com.capstone.Capstone_2.controller;

import com.capstone.Capstone_2.dto.RecommendationTemplateDto;
import com.capstone.Capstone_2.dto.TemplateSpotDto; // TemplateSpotDto import
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/courses/recommendation-templates") // ✅ /api 경로 제거
public class RecommendationTemplateController {

    @GetMapping
    public List<RecommendationTemplateDto> list() {
        return List.of(
                RecommendationTemplateDto.builder()
                        .code("MORNING_BUDGET")
                        .title("아침 3스팟(저예산)")
                        .summary("아침 산책 → 간단 브런치 → 무료 전시")
                        .spots(List.of(
                                // ✅ TemplateSpotDto 사용 및 필드명 통일
                                TemplateSpotDto.builder().title("근린공원 산책").stayMinutes(60).price(0).build(),
                                TemplateSpotDto.builder().title("브런치 카페").stayMinutes(90).price(8000).build(),
                                TemplateSpotDto.builder().title("구청 무료전시").stayMinutes(60).price(0).build()
                        ))
                        .build(),
                RecommendationTemplateDto.builder()
                        .code("DATE_STANDARD")
                        .title("데이트 4스팟(표준예산)")
                        .summary("카페 → 전시 → 맛집 → 야경")
                        .spots(List.of(
                                TemplateSpotDto.builder().title("감성 카페").stayMinutes(90).price(7000).build(),
                                TemplateSpotDto.builder().title("작은 미술관").stayMinutes(120).price(12000).build(),
                                TemplateSpotDto.builder().title("지역 맛집").stayMinutes(90).price(15000).build(),
                                TemplateSpotDto.builder().title("야경 스팟").stayMinutes(60).price(0).build()
                        ))
                        .build()
        );
    }
}