package com.capstone.Capstone_2.service;

import com.capstone.Capstone_2.dto.CourseDto.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.UUID;


public interface CourseService {
    Detail create(CreateReq req, UUID creatorUserId);
    Detail update(UUID courseId, UpdateReq req, UUID currentUserId);
    void delete(UUID courseId, UUID currentUserId);
    Detail get(UUID courseId);
    Page<CourseSummary> search(String q, Pageable pageable);
    Detail submitForReview(UUID courseId, UUID currentUserId);
    Detail approve(UUID courseId);
    Detail reject(UUID courseId, String reason);
}
