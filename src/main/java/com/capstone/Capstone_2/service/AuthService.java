package com.capstone.Capstone_2.service;

import com.capstone.Capstone_2.dto.LoginDto;
import com.capstone.Capstone_2.dto.SignUpDto;
import com.capstone.Capstone_2.dto.TokenDto;

public interface AuthService {
    void signup(SignUpDto signUpDto);
    TokenDto login(LoginDto loginDto);
}