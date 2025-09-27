package com.capstone.Capstone_2.dto;

import com.capstone.Capstone_2.entity.User;
import lombok.Builder;
import lombok.Getter;

import java.util.Map;

@Getter
public class OAuthAttributes {
    private Map<String, Object> attributes;
    private String nameAttributeKey;
    // ✅ 1. User 엔티티에 저장할 핵심 필드 추가
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
                // ✅ 2. 구글이 주는 정보에서 nickname, email, avatarUrl 추출
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
                // ✅ 2. 카카오가 주는 중첩된 정보에서 nickname, email, avatarUrl 추출
                .nickname((String) profile.get("nickname"))
                .email((String) kakaoAccount.get("email"))
                .avatarUrl((String) profile.get("profile_image_url"))
                .provider("kakao")
                .providerId(String.valueOf(attributes.get(userNameAttributeName)))
                .attributes(attributes)
                .nameAttributeKey(userNameAttributeName)
                .build();
    }

    // ✅ 3. 처음 가입하는 사용자를 위한 User 엔티티 생성 메서드 추가
    public User toEntity() {
        return User.builder()
                .nickname(nickname)
                .email(email)
                .avatarUrl(avatarUrl)
                .provider(provider)
                .providerId(providerId)
                .role("user")
                .status("active")
                .build();
    }
}