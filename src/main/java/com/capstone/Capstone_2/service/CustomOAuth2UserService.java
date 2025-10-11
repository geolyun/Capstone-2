package com.capstone.Capstone_2.service;

import com.capstone.Capstone_2.dto.OAuthAttributes;
import com.capstone.Capstone_2.entity.CreatorProfile;
import com.capstone.Capstone_2.entity.User;
import com.capstone.Capstone_2.entity.UserRole;
import com.capstone.Capstone_2.repository.UserRepository;
import com.capstone.Capstone_2.dto.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

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

        return new UserPrincipal(user, attributes.getAttributes());
    }

    private User saveOrUpdate(OAuthAttributes attributes) {
        if (!StringUtils.hasText(attributes.getEmail())) {
            throw new OAuth2AuthenticationException("Email not found from OAuth2 provider.");
        }

        Optional<User> userOptional = userRepository.findByEmail(attributes.getEmail());
        User user;

        if (userOptional.isPresent()) {
            // --- 이미 존재하는 사용자일 경우 ---
            user = userOptional.get();

            // ✅✅✅ 닉네임 덮어쓰기 방지 로직 ✅✅✅
            // 만약 사용자가 다른 소셜 프로필 이미지로 업데이트하는 것을 허용하고 싶다면,
            // 아래 아바타 URL 업데이트 코드는 남겨둘 수 있습니다.
            // user.setAvatarUrl(attributes.getAvatarUrl());

        } else {
            // --- 존재하지 않는 신규 사용자일 경우 ---
            user = attributes.toEntity();
            // 신규 사용자는 GUEST 역할로 설정하여 닉네임 설정 페이지로 유도
            user.setRole(UserRole.GUEST);

            CreatorProfile newProfile = CreatorProfile.builder()
                    .user(user)
                    // 소셜 프로필의 이름을 기본 displayName으로 설정
                    .displayName(attributes.getNickname())
                    .build();

            user.setCreatorProfile(newProfile);
        }

        return userRepository.save(user);
    }
}