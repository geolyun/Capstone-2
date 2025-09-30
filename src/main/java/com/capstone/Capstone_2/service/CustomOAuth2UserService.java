package com.capstone.Capstone_2.service;

import com.capstone.Capstone_2.dto.OAuthAttributes;
import com.capstone.Capstone_2.entity.CreatorProfile;
import com.capstone.Capstone_2.entity.User;
import com.capstone.Capstone_2.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);

        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        String userNameAttributeName = userRequest.getClientRegistration().getProviderDetails()
                .getUserInfoEndpoint().getUserNameAttributeName();

        OAuthAttributes attributes = OAuthAttributes.of(registrationId, userNameAttributeName, oAuth2User.getAttributes());

        User user = saveOrUpdate(attributes);

        return new DefaultOAuth2User(
                Collections.singleton(new SimpleGrantedAuthority("ROLE_" + user.getRole().toUpperCase())),
                attributes.getAttributes(),
                attributes.getNameAttributeKey()
        );
    }

    // ✅✅✅ 이 메서드의 로직을 완전히 변경합니다. ✅✅✅
    private User saveOrUpdate(OAuthAttributes attributes) {
        // 1. 이메일 누락 문제 해결: 이메일이 없는 경우 예외 발생
        if (!StringUtils.hasText(attributes.getEmail())) {
            throw new OAuth2AuthenticationException("Email not found from OAuth2 provider.");
        }

        // 2. 이메일 중복 문제 해결: providerId가 아닌, email로 먼저 사용자를 찾습니다.
        Optional<User> userOptional = userRepository.findByEmail(attributes.getEmail());
        User user;

        if (userOptional.isPresent()) {
            // --- 이미 존재하는 사용자일 경우 ---
            user = userOptional.get();
            // 필요 시 닉네임, 프로필 이미지 등 소셜 로그인 정보 업데이트
            user.setNickname(attributes.getNickname());
            user.setAvatarUrl(attributes.getAvatarUrl());

        } else {
            // --- 존재하지 않는 신규 사용자일 경우 ---
            // 'user' 변수에 새로 생성한 User 객체를 할당합니다.
            user = attributes.toEntity();

            // ✅ 'newUser' 대신 'user' 변수를 사용하도록 수정합니다.
            CreatorProfile newProfile = CreatorProfile.builder()
                    .user(user)
                    .displayName(attributes.getNickname())
                    .build();

            // ✅ 'newUser' 대신 'user' 변수를 사용하도록 수정합니다.
            user.setCreatorProfile(newProfile);
        }

        // ✅ if-else 블록 바깥에서 최종적으로 user 객체를 한 번만 저장합니다.
        return userRepository.save(user);
    }
}