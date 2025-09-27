package com.capstone.Capstone_2.repository;

import com.capstone.Capstone_2.entity.Course;
import com.capstone.Capstone_2.entity.CourseSpot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CourseSpotRepository extends JpaRepository<CourseSpot, UUID> {
    List<CourseSpot> findByCourseOrderByOrderNoAsc(Course course);
    void deleteByCourse(Course course);
}
