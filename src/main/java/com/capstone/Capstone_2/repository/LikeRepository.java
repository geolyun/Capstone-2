package com.capstone.Capstone_2.repository;

import com.capstone.Capstone_2.entity.Like;
import com.capstone.Capstone_2.entity.LikeId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LikeRepository extends JpaRepository<Like, LikeId> {
}
