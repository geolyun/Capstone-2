package com.capstone.Capstone_2.service.impl;

import com.capstone.Capstone_2.dto.SignUpDto;
import com.capstone.Capstone_2.entity.User;
import com.capstone.Capstone_2.repository.UserRepository;
import com.capstone.Capstone_2.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder; // SecurityConfig에 Bean으로 등록된 객체가 주입됩니다.

    @Override
    @Transactional
    public User registerNewUser(SignUpDto signUpDto) {
        // 이메일 중복 확인
        if (userRepository.findByEmail(signUpDto.getEmail()).isPresent()) {
            throw new IllegalStateException("이미 사용 중인 이메일입니다.");
        }

        // DTO를 User 엔티티로 변환
        User newUser = User.builder()
                .email(signUpDto.getEmail())
                .passwordHash(passwordEncoder.encode(signUpDto.getPassword())) // 비밀번호 암호화
                .nickname(signUpDto.getNickname())
                .provider("local") // 로컬 회원가입
                .role("user")      // 기본 역할
                .status("active")  // 기본 상태
                .build();

        return userRepository.save(newUser);
    }
}