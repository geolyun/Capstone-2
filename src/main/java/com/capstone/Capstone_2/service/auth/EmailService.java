package com.capstone.Capstone_2.service.auth;

import com.capstone.Capstone_2.entity.User;

public interface EmailService {
    void sendVerificationCode(User user, String code);
}
