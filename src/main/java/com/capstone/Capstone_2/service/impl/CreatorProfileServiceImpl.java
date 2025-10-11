package com.capstone.Capstone_2.service.impl;

import com.capstone.Capstone_2.dto.CreatorProfileDto;
import com.capstone.Capstone_2.entity.CreatorProfile;
import com.capstone.Capstone_2.entity.User;
import com.capstone.Capstone_2.repository.UserRepository;
import com.capstone.Capstone_2.dto.UserPrincipal;
import com.capstone.Capstone_2.service.CreatorProfileService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CreatorProfileServiceImpl implements CreatorProfileService {

    private final UserRepository userRepository;

    @Override
    @Transactional
    public void updateProfile(String userEmail, CreatorProfileDto.UpdateRequest updateRequest) {

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new EntityNotFoundException("사용자를 찾을 수 없습니다."));

        CreatorProfile profile = user.getCreatorProfile();
        if (profile == null) {
            throw new EntityNotFoundException("크리에이터 프로필을 찾을 수 없습니다.");
        }

        profile.setDisplayName(updateRequest.getDisplayName());
        profile.setBio(updateRequest.getBio());

        if (updateRequest.getAvatarUrl() != null && !updateRequest.getAvatarUrl().isBlank()) {
            user.setAvatarUrl(updateRequest.getAvatarUrl());
        }

        Authentication currentAuth = SecurityContextHolder.getContext().getAuthentication();
        if (currentAuth != null && currentAuth.getPrincipal() instanceof UserPrincipal) {
            UserPrincipal newUserPrincipal = new UserPrincipal(user);
            Authentication newAuth = new UsernamePasswordAuthenticationToken(
                    newUserPrincipal,
                    currentAuth.getCredentials(),
                    newUserPrincipal.getAuthorities()
            );
            SecurityContextHolder.getContext().setAuthentication(newAuth);
        }
    }
}