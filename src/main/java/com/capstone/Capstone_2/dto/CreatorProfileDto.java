package com.capstone.Capstone_2.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

public class CreatorProfileDto {

    @Getter
    @Setter
    public static class UpdateRequest {
        @NotBlank(message = "표시용 이름은 비워둘 수 없습니다.")
        @Size(max = 60, message = "이름은 60자를 초과할 수 없습니다.")
        private String displayName;

        private String bio;

        private String avatarUrl;
    }
}