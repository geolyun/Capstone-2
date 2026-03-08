package com.capstone.Capstone_2.dto;

import com.capstone.Capstone_2.entity.User;

import java.util.UUID;

public record ProfileDto(
        UUID id,
        String email,
        String nickname,
        String displayName,
        String bio,
        String avatarUrl
) {
    public static ProfileDto from(User user) {
        return new ProfileDto(
                user.getCreatorProfile() != null ? user.getCreatorProfile().getId() : null,
                user.getEmail(),
                user.getNickname(),
                user.getCreatorProfile() != null ? user.getCreatorProfile().getDisplayName() : user.getNickname(),
                user.getCreatorProfile() != null ? user.getCreatorProfile().getBio() : "",
                user.getAvatarUrl()
        );
    }
}
