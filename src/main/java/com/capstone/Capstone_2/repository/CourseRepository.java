package com.capstone.Capstone_2.repository;

import com.capstone.Capstone_2.entity.Course;
import com.capstone.Capstone_2.entity.ReviewState;
import com.capstone.Capstone_2.entity.CreatorProfile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;


public interface CourseRepository extends JpaRepository<Course, UUID> {
    Page<Course> findByReviewStateAndRegionCodeInAndCategory_SlugIn(
            ReviewState state, Iterable<String> regionCodes, Iterable<String> categorySlugs, Pageable pageable);


    Page<Course> findByCreator(CreatorProfile creator, Pageable pageable);


    Page<Course> findByOrderByLikeCountDesc(Pageable pageable);

    @Query("select c from Course c where c.reviewState = :state and (:q is null or lower(c.title) like lower(concat('%', :q, '%')) or lower(c.summary) like lower(concat('%', :q, '%')))")
    Page<Course> search(@Param("state") ReviewState state, @Param("q") String q, Pageable pageable);

    @Query("SELECT l2.course FROM Like l1 " +
            "JOIN Like l2 ON l1.user = l2.user " +
            "WHERE l1.course.id = :courseId AND l1.course.id != l2.course.id " +
            "GROUP BY l2.course " +
            "ORDER BY COUNT(l2.course) DESC")
    List<Course> findRelatedCoursesByLikes(@Param("courseId") UUID courseId, Pageable pageable);
}