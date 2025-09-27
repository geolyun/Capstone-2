package com.capstone.Capstone_2.repository;

import com.capstone.Capstone_2.entity.CreatorProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CreatorProfileRepository extends JpaRepository<CreatorProfile, UUID> {
    Optional<CreatorProfile> findByUserId(UUID userId);
}

