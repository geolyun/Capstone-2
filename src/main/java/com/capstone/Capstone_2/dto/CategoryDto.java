package com.capstone.Capstone_2.dto;

import java.util.UUID;

public record CategoryDto(UUID id, String name, String slug, UUID parentId) {}