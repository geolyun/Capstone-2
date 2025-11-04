// src/main/java/com/capstone/Capstone_2/controller/RankingController.java
package com.capstone.Capstone_2.controller;

import com.capstone.Capstone_2.dto.CourseDto.CourseSummary;
import com.capstone.Capstone_2.service.CourseService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/courses")
@RequiredArgsConstructor
public class RankingController {

    private final CourseService courseService;

    @GetMapping("/popular")
    public Page<CourseSummary> popular(Pageable pageable) {
        return courseService.getPopularCourses(pageable);
    }
}
