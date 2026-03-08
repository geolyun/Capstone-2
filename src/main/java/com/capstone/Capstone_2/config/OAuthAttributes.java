package com.capstone.Capstone_2.config;

import com.capstone.Capstone_2.entity.User;
import com.capstone.Capstone_2.entity.UserRole;
import com.capstone.Capstone_2.entity.UserStatus;
import lombok.Builder;
import lombok.Getter;

import java.util.Map;

@Getter
public class OAuthAttributes {
    private Map<String, Object> attributes;
    private String nameAttributeKey;
    private String nickname;
    private String email;
    private String avatarUrl;
    private String provider;
    private String providerId;

    @Builder
    public OAuthAttributes(Map<String, Object> attributes, String nameAttributeKey,
                           String nickname, String email, String avatarUrl, String provider, String providerId) {
        this.attributes = attributes;
        this.nameAttributeKey = nameAttributeKey;
        this.nickname = nickname;
        this.email = email;
        this.avatarUrl = avatarUrl;
        this.provider = provider;
        this.providerId = providerId;
    }

    public static OAuthAttributes of(String registrationId, String userNameAttributeName, Map<String, Object> attributes) {
        if ("kakao".equals(registrationId)) {
            return ofKakao(userNameAttributeName, attributes);
        }
        return ofGoogle(userNameAttributeName, attributes);
    }

    private static OAuthAttributes ofGoogle(String userNameAttributeName, Map<String, Object> attributes) {
        return OAuthAttributes.builder()
                .nickname((String) attributes.get("name"))
                .email((String) attributes.get("email"))
                .avatarUrl((String) attributes.get("picture"))
                .provider("google")
                .providerId((String) attributes.get(userNameAttributeName))
                .attributes(attributes)
                .nameAttributeKey(userNameAttributeName)
                .build();
    }

    private static OAuthAttributes ofKakao(String userNameAttributeName, Map<String, Object> attributes) {
        Map<String, Object> kakaoAccount = (Map<String, Object>) attributes.get("kakao_account");
        Map<String, Object> profile = (Map<String, Object>) kakaoAccount.get("profile");

        return OAuthAttributes.builder()
                .nickname((String) profile.get("nickname"))
                .email((String) kakaoAccount.get("email"))
                .avatarUrl((String) profile.get("profile_image_url"))
                .provider("kakao")
                .providerId(String.valueOf(attributes.get(userNameAttributeName)))
                .attributes(attributes)
                .nameAttributeKey(userNameAttributeName)
                .build();
    }

    public User toEntity() {
        return User.builder()
                .nickname(nickname)
                .email(email)
                .avatarUrl(avatarUrl)
                .provider(provider)
                .providerId(providerId)
                .role(UserRole.USER)
                .status(UserStatus.ACTIVE)
                .build();
    }
}