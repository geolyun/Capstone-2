package com.capstone.Capstone_2.service.impl;

import com.capstone.Capstone_2.config.JwtUtil;
import com.capstone.Capstone_2.dto.LoginDto;
import com.capstone.Capstone_2.dto.SignUpDto;
import com.capstone.Capstone_2.dto.TokenDto;
import com.capstone.Capstone_2.entity.User;
import com.capstone.Capstone_2.entity.UserRole;
import com.capstone.Capstone_2.repository.UserRepository;
import com.capstone.Capstone_2.service.AuthService;
import com.capstone.Capstone_2.entity.CreatorProfile;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;

    @Override
    @Transactional
    public void signup(SignUpDto signUpDto) {
        if (!signUpDto.getPassword().equals(signUpDto.getPasswordConfirm())) {
            throw new IllegalArgumentException("비밀번호가 일치하지 않습니다.");
        }
        if (userRepository.findByEmail(signUpDto.getEmail()).isPresent()) {
            throw new IllegalStateException("이미 사용 중인 이메일입니다.");
        }

        User newUser = User.builder()
                .email(signUpDto.getEmail())
                .passwordHash(passwordEncoder.encode(signUpDto.getPassword()))
                .nickname(signUpDto.getNickname())
                .provider("local")
                .role(UserRole.USER)
                .status("active")
                .build();

        // ✅ 1. 새로운 CreatorProfile을 생성하고 User와 연결합니다.
        CreatorProfile newProfile = CreatorProfile.builder()
                .user(newUser)
                .displayName(newUser.getNickname()) // 초기 표시명은 닉네임으로 설정
                .build();

        // ✅ 2. User 엔티티에 생성된 프로필을 설정합니다.
        newUser.setCreatorProfile(newProfile);

        // ✅ 3. User를 저장하면 Cascade 설정에 따라 CreatorProfile도 함께 저장됩니다.
        userRepository.save(newUser);
    }

    @Override
    @Transactional(readOnly = true)
    public TokenDto login(LoginDto loginDto) {
        // ✅ SecurityConfig에 등록한 AuthenticationManager를 사용하여 인증 수행
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginDto.getEmail(), loginDto.getPassword())
        );

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String token = jwtUtil.generateToken(userDetails);
        return new TokenDto(token);
    }
}