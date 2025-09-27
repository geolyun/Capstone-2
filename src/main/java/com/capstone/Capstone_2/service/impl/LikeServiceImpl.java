package com.capstone.Capstone_2.service.impl;

import com.capstone.Capstone_2.entity.Course;
import com.capstone.Capstone_2.entity.Like;
import com.capstone.Capstone_2.entity.LikeId;
import com.capstone.Capstone_2.entity.User;
import com.capstone.Capstone_2.dto.LikeDto;
import com.capstone.Capstone_2.repository.CourseRepository;
import com.capstone.Capstone_2.repository.LikeRepository;
import com.capstone.Capstone_2.repository.UserRepository;
import com.capstone.Capstone_2.service.LikeService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.util.UUID;


@Service @RequiredArgsConstructor @Transactional
public class LikeServiceImpl implements LikeService {
    private final LikeRepository likeRepo;
    private final UserRepository userRepo;
    private final CourseRepository courseRepo;


    @Override
    public LikeDto toggle(UUID userId, UUID courseId) {
        User u = userRepo.findById(userId).orElseThrow(EntityNotFoundException::new);
        Course c = courseRepo.findById(courseId).orElseThrow(EntityNotFoundException::new);


        var id = new LikeId(userId, courseId);
        if (likeRepo.existsById(id)) {
            likeRepo.deleteById(id);
            c.setLikeCount(Math.max(0, c.getLikeCount() - 1));
            return new LikeDto(userId, courseId, false);
        } else {
            likeRepo.save(Like.builder().id(id).user(u).course(c).build());
            c.setLikeCount(c.getLikeCount() + 1);
            return new LikeDto(userId, courseId, true);
        }
    }
}