package com.capstone.Capstone_2.dto;

import java.util.UUID;

public record UserDto(UUID id, String email, String nickname, String avatarUrl, String role, String status) {}