package com.capstone.Capstone_2.service;

import com.capstone.Capstone_2.dto.LikeDto;
import java.util.UUID;


public interface LikeService {
    LikeDto toggle(UUID userId, UUID courseId);
}
