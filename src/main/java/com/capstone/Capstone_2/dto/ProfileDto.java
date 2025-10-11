package com.capstone.Capstone_2.dto;

import com.capstone.Capstone_2.entity.User;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ProfileDto {
    private String email;
    private String nickname;
    private String displayName;
    private String bio;
    private String avatarUrl;

    public static ProfileDto from(User user) {
        return ProfileDto.builder()
                .email(user.getEmail())
                .nickname(user.getNickname())
                .displayName(user.getCreatorProfile() != null ? user.getCreatorProfile().getDisplayName() : user.getNickname())
                .bio(user.getCreatorProfile() != null ? user.getCreatorProfile().getBio() : "")
                .avatarUrl(user.getAvatarUrl())
                .build();
    }
}