package com.capstone.Capstone_2.service.auth;

import com.capstone.Capstone_2.dto.LoginDto;
import com.capstone.Capstone_2.dto.SignUpDto;
import com.capstone.Capstone_2.dto.TokenDto;
import com.capstone.Capstone_2.service.user.UserService; // ✅ UserService 추가
import com.capstone.Capstone_2.config.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final UserService userService;

    @Override
    @Transactional
    public void signup(SignUpDto signUpDto) {
        userService.registerNewUser(signUpDto);
    }

    @Override
    @Transactional(readOnly = true)
    public TokenDto login(LoginDto loginDto) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginDto.getEmail(), loginDto.getPassword())
        );

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();

        String token = jwtUtil.generateToken(userDetails);
        return new TokenDto(token);
    }
}