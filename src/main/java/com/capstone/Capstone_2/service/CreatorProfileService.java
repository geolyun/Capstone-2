package com.capstone.Capstone_2.service;

import com.capstone.Capstone_2.dto.CreatorProfileDto;

public interface CreatorProfileService {

    void updateProfile(String userEmail, CreatorProfileDto.UpdateRequest updateRequest);
}
