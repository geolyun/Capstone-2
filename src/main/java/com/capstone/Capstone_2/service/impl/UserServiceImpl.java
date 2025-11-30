package com.capstone.Capstone_2.service.impl;

import com.capstone.Capstone_2.dto.SignUpDto;
import com.capstone.Capstone_2.entity.CreatorProfile;
import com.capstone.Capstone_2.entity.User;
import com.capstone.Capstone_2.entity.UserRole;
import com.capstone.Capstone_2.entity.UserStatus;
import com.capstone.Capstone_2.repository.UserRepository;
import com.capstone.Capstone_2.service.EmailService;
import com.capstone.Capstone_2.service.UserService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService; // ✅ EmailService 주입 확인
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

        if (userRepository.existsByNickname(signUpDto.getNickname())) {
            throw new IllegalStateException("이미 사용 중인 닉네임입니다.");
        }

        // ✅ 6자리 인증 코드 생성
        String code = createVerificationCode();

        User newUser = User.builder()
                .email(signUpDto.getEmail())
                .passwordHash(passwordEncoder.encode(signUpDto.getPassword()))
                .nickname(signUpDto.getNickname())
                .provider("local")
                .role(UserRole.USER)
                .status(UserStatus.PENDING) // ✅ PENDING 상태
                .verificationToken(code) // ✅ 6자리 코드 저장
                .tokenExpiryDate(LocalDateTime.now().plusMinutes(5)) // ✅ 5분 만료
                .build();

        CreatorProfile newProfile = CreatorProfile.builder()
                .user(newUser)
                .displayName(newUser.getNickname())
                .build();

        newUser.setCreatorProfile(newProfile);
        User savedUser = userRepository.save(newUser);

        // ✅ 인증 "코드" 발송 (메서드명 변경은 2단계에서)
        emailService.sendVerificationCode(savedUser, code);

        return savedUser;
    }

    // ✅ (수정) `verifyEmail`을 `verifyCode`로 변경
    @Override
    @Transactional
    public boolean verifyCode(String email, String code) {
        User user = userRepository.findByEmail(email)
                .orElse(null);

        // 1. 사용자가 없거나, PENDING 상태가 아닌지 확인
        if (user == null || !user.getStatus().equals(UserStatus.PENDING)) {
            logger.warn("인증 코드 확인 실패: 사용자를 찾을 수 없거나 PENDING 상태가 아님. Email: {}", email);
            return false;
        }

        // 2. 토큰 만료 시간 확인
        if (user.getTokenExpiryDate().isBefore(LocalDateTime.now())) {
            logger.warn("인증 코드 확인 실패: 코드 만료. User: {}", user.getEmail());
            // TODO: (선택) 여기서 새 코드를 생성하고 재발송하는 로직을 추가할 수 있습니다.
            return false;
        }

        // 3. 코드 일치 여부 확인
        if (user.getVerificationToken() == null || !user.getVerificationToken().equals(code)) {
            logger.warn("인증 코드 확인 실패: 코드가 일치하지 않음. User: {}", user.getEmail());
            return false;
        }

        // 4. 인증 성공: 계정 활성화
        user.setStatus(UserStatus.ACTIVE);
        user.setVerificationToken(null); // 사용한 토큰 삭제
        user.setTokenExpiryDate(null);
        userRepository.save(user);

        logger.info("이메일 인증 성공 (코드로 활성화). User: {}", user.getEmail());
        return true;
    }

    // ✅ (신규) 6자리 인증 코드 생성 헬퍼
    private String createVerificationCode() {
        Random random = new Random();
        int code = 100000 + random.nextInt(900000); // 100000 ~ 999999
        return String.valueOf(code);
    }
}