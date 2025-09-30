package com.capstone.Capstone_2.service.impl;

import com.capstone.Capstone_2.dto.SignUpDto;
import com.capstone.Capstone_2.entity.CreatorProfile;
import com.capstone.Capstone_2.entity.User;
import com.capstone.Capstone_2.repository.UserRepository;
import com.capstone.Capstone_2.service.UserService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private static final Logger logger = LoggerFactory.getLogger(UserServiceImpl.class);

    @Override
    @Transactional
    public User registerNewUser(SignUpDto signUpDto) {
        if (!signUpDto.getPassword().equals(signUpDto.getPasswordConfirm())) {
            throw new IllegalArgumentException("비밀번호가 일치하지 않습니다.");
        }

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

        CreatorProfile newProfile = CreatorProfile.builder()
                .user(newUser) // ✅ User와 연결
                .displayName(newUser.getNickname()) // ✅ 우선 닉네임을 표시명으로 사용
                .build();

        // ✅ 2. User 엔티티에 생성된 프로필을 설정합니다.
        newUser.setCreatorProfile(newProfile);

        // ✅ 3. User를 저장하면, User 엔티티의 Cascade 설정에 의해 CreatorProfile도 함께 저장됩니다.
        return userRepository.save(newUser);
    }
}