package com.capstone.Capstone_2.dto;

import com.capstone.Capstone_2.entity.Region;
import lombok.Getter;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Getter
public class RegionDto {
    private UUID id;
    private String name;
    private String code; // 예: 11, 11680
    private List<RegionDto> children;

    public RegionDto(Region region) {
        this.id = region.getId();
        this.name = region.getName();
        this.code = region.getCode();
        // ✅ 자식 지역(시/군/구)들도 자동으로 DTO로 변환되어 리스트에 담김 (재귀)
        this.children = region.getChildren().stream()
                .map(RegionDto::new)
                .collect(Collectors.toList());
    }
}
