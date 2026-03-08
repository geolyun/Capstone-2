package com.capstone.Capstone_2.repository;

import com.capstone.Capstone_2.dto.CourseSearchDto;
import com.capstone.Capstone_2.entity.Course;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

// QueryDSL을 사용한 동적 쿼리여서 분리
public interface CourseRepositoryCustom {
    Page<Course> searchByFilter(CourseSearchDto dto, Pageable pageable);
}
