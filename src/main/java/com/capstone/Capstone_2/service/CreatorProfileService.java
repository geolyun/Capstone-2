package com.capstone.Capstone_2.service;

import com.capstone.Capstone_2.dto.CreatorProfileDto;
import com.capstone.Capstone_2.dto.ProfileDto;

public interface CreatorProfileService {

    ProfileDto getProfile(String userEmail);

    void updateProfile(String userEmail, CreatorProfileDto.UpdateRequest updateRequest);
}
