package com.capstone.Capstone_2.service;

import com.capstone.Capstone_2.dto.CourseDto.*;
import com.capstone.Capstone_2.dto.CourseSearchDto;
import com.capstone.Capstone_2.dto.RecommendationDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface CourseService {
    Detail create(CreateReq req, String creatorEmail);
    Detail update(UUID courseId, UpdateReq req, String currentUserEmail);
    void delete(UUID courseId, String currentUserEmail);
    Detail get(UUID courseId);
    Page<CourseSummary> search(CourseSearchDto searchDto, Pageable pageable);
    Detail submitForReview(UUID courseId, String currentUserEmail);
    Detail approve(UUID courseId);
    Detail reject(UUID courseId, String reason);
    Page<CourseSummary> getPopularCourses(Pageable pageable);
    RecommendationDto getCourseRecommendations(UUID courseId);
}