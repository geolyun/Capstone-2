package com.capstone.Capstone_2.dto;

import com.capstone.Capstone_2.entity.User;
import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
public class ProfileDto {
    private UUID id;
    private String email;
    private String nickname;
    private String displayName;
    private String bio;
    private String avatarUrl;

    public static ProfileDto from(User user) {
        return ProfileDto.builder()
                .id(user.getCreatorProfile() != null ? user.getCreatorProfile().getId() : null)
                .email(user.getEmail())
                .nickname(user.getNickname())
                .displayName(user.getCreatorProfile() != null ? user.getCreatorProfile().getDisplayName() : user.getNickname())
                .bio(user.getCreatorProfile() != null ? user.getCreatorProfile().getBio() : "")
                .avatarUrl(user.getAvatarUrl())
                .build();
    }
}