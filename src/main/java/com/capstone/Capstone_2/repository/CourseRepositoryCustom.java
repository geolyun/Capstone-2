package com.capstone.Capstone_2.repository;

import com.capstone.Capstone_2.dto.CourseSearchDto;
import com.capstone.Capstone_2.entity.Course;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CourseRepositoryCustom {
    Page<Course> searchByFilter(CourseSearchDto dto, Pageable pageable);
}
