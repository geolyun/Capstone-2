package com.capstone.Capstone_2.service;

import com.capstone.Capstone_2.dto.LikeDto;
import java.util.UUID;


public interface LikeService {
    LikeDto toggleLike(UUID courseId, String userEmail);

    boolean isCourseLikedByUser(UUID courseId, String userEmail);
}
