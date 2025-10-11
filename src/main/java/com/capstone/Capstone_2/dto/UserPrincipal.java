package com.capstone.Capstone_2.dto;

import com.capstone.Capstone_2.entity.User;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User; // ✅ OAuth2User import

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;

@Getter
public class UserPrincipal implements UserDetails, OAuth2User {

    private final User user;
    private final UUID id;
    private final String email;
    private final String password;
    private final String nickname;
    private final Collection<? extends GrantedAuthority> authorities;
    private Map<String, Object> attributes; // ✅ 소셜 로그인 속성을 담을 필드

    // 1. 일반 로그인용 생성자
    public UserPrincipal(User user) {
        this.user = user;
        this.id = user.getId();
        this.email = user.getEmail();
        this.password = user.getPasswordHash();
        this.nickname = user.getNickname();
        this.authorities = Collections.singletonList(
                new SimpleGrantedAuthority("ROLE_" + user.getRole().name())
        );
    }

    // ✅ 2. 소셜 로그인용 생성자 추가
    public UserPrincipal(User user, Map<String, Object> attributes) {
        this(user); // 위 생성자 호출
        this.attributes = attributes;
    }

    @Override
    public String getUsername() { return this.email; }

    @Override
    public String getPassword() { return this.password; }

    @Override
    public Map<String, Object> getAttributes() {
        return this.attributes;
    }

    @Override
    public String getName() {
        return this.user.getProviderId();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() { return this.authorities; }

    @Override
    public boolean isAccountNonExpired() { return true; }

    @Override
    public boolean isAccountNonLocked() { return true; }

    @Override
    public boolean isCredentialsNonExpired() { return true; }

    @Override
    public boolean isEnabled() { return "active".equalsIgnoreCase(this.user.getStatus()); }
}