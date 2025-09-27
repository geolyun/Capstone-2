package com.capstone.Capstone_2.service;

import com.capstone.Capstone_2.dto.SignUpDto;
import com.capstone.Capstone_2.entity.User;

public interface UserService {
    User registerNewUser(SignUpDto signUpDto);
}