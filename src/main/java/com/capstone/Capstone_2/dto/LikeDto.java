package com.capstone.Capstone_2.dto;

import java.util.UUID;

public record LikeDto(UUID userId, UUID courseId, boolean liked) {}