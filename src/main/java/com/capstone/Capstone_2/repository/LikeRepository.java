package com.capstone.Capstone_2.repository;

import com.capstone.Capstone_2.entity.Course;
import com.capstone.Capstone_2.entity.Like;
import com.capstone.Capstone_2.entity.LikeId;
import com.capstone.Capstone_2.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LikeRepository extends JpaRepository<Like, LikeId> {
    List<Like> findByUser(User user);
    boolean existsByUserAndCourse(User user, Course course);
    long countByCourse(Course course);
}
