package com.capstone.Capstone_2.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TemplateSpotDto {
    private String title;
    private Integer stayMinutes; // duration -> stayMinutes
    private Integer price;       // cost -> price
    private String description;
}