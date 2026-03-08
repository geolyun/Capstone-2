package com.capstone.Capstone_2.service.user;

import com.capstone.Capstone_2.config.OAuthAttributes;
import com.capstone.Capstone_2.config.security.UserPrincipal;
import com.capstone.Capstone_2.entity.CreatorProfile;
import com.capstone.Capstone_2.entity.User;
import com.capstone.Capstone_2.entity.UserRole;
import com.capstone.Capstone_2.repository.UserRepository;

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
            user = userOptional.get();

        } else {
            user = attributes.toEntity();
            // 신규 사용자는 GUEST 역할로 설정하여 닉네임 설정 페이지로 유도
            user.setRole(UserRole.GUEST);

            CreatorProfile newProfile = CreatorProfile.builder()
                    .user(user)
                    .displayName(attributes.getNickname())
                    .build();

            user.setCreatorProfile(newProfile);
        }

        return userRepository.save(user);
    }
}