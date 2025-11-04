package com.capstone.Capstone_2.service.impl;

import com.capstone.Capstone_2.dto.LikeDto;
import com.capstone.Capstone_2.entity.Course;
import com.capstone.Capstone_2.entity.Like;
import com.capstone.Capstone_2.entity.LikeId;
import com.capstone.Capstone_2.entity.User;
import com.capstone.Capstone_2.repository.CourseRepository;
import com.capstone.Capstone_2.repository.LikeRepository;
import com.capstone.Capstone_2.repository.UserRepository;
import com.capstone.Capstone_2.service.LikeService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict; // ✅ CacheEvict import
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LikeServiceImpl implements LikeService {

    private final LikeRepository likeRepo;
    private final UserRepository userRepo;
    private final CourseRepository courseRepo;

    @Override
    @Transactional
    @CacheEvict(value = "popularCourses", allEntries = true) // ✅ 캐시 무효화 기능 추가
    public LikeDto toggleLike(UUID courseId, String userEmail) {
        User user = userRepo.findByEmail(userEmail)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));
        Course course = courseRepo.findById(courseId)
                .orElseThrow(() -> new EntityNotFoundException("Course not found"));

        LikeId likeId = new LikeId(user.getId(), courseId);

        if (likeRepo.existsById(likeId)) {
            // '좋아요' 취소
            likeRepo.deleteById(likeId);
            course.setLikeCount(Math.max(0, course.getLikeCount() - 1));
            return new LikeDto(user.getId(), courseId, false, course.getLikeCount());
        } else {
            // '좋아요' 추가
            Like newLike = Like.builder().id(likeId).user(user).course(course).build();
            likeRepo.save(newLike);
            course.setLikeCount(course.getLikeCount() + 1);
            return new LikeDto(user.getId(), courseId, true, course.getLikeCount());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isCourseLikedByUser(UUID courseId, String userEmail) {
        Optional<User> userOpt = userRepo.findByEmail(userEmail);
        if (userOpt.isEmpty()) {
            return false;
        }
        User user = userOpt.get();

        if (!courseRepo.existsById(courseId)) {
            return false;
        }

        LikeId likeId = new LikeId(user.getId(), courseId);
        return likeRepo.existsById(likeId);
    }
}