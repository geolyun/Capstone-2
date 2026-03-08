package com.capstone.Capstone_2.repository;

import com.capstone.Capstone_2.config.QueryDslConfig;
import com.capstone.Capstone_2.entity.CreatorProfile;
import com.capstone.Capstone_2.entity.User;
import com.capstone.Capstone_2.entity.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest // JPA 관련 컴포넌트만 로드 (인메모리 DB 사용)
@Import(QueryDslConfig.class)
class UserRepositoryTest {

    @Autowired
    private TestEntityManager em; // 테스트용 EntityManager

    @Autowired
    private UserRepository userRepository;

    private User localUser;
    private User googleUser;

    @BeforeEach
    void setUp() {
        // 1. 로컬 가입 사용자 (CreatorProfile과 함께)
        localUser = User.builder()
                .email("local@user.com")
                .nickname("LocalUser")
                .passwordHash("hashed_password")
                .provider("local")
                .role(UserRole.USER)
                .status("active")
                .build();
        CreatorProfile localProfile = CreatorProfile.builder().user(localUser).displayName("LocalUser").build();
        localUser.setCreatorProfile(localProfile);
        em.persist(localUser); // Cascade 설정으로 localProfile도 저장됨

        // 2. 소셜 로그인 사용자
        googleUser = User.builder()
                .email("google@user.com")
                .nickname("GoogleUser")
                .provider("google")
                .providerId("google_123456")
                .role(UserRole.USER)
                .status("active")
                .build();
        CreatorProfile googleProfile = CreatorProfile.builder().user(googleUser).displayName("GoogleUser").build();
        googleUser.setCreatorProfile(googleProfile);
        em.persist(googleUser);

        em.flush(); // DB 반영
    }

    @Test
    @DisplayName("findByEmail - 성공 (사용자 존재)")
    void findByEmail_success_whenUserExists() {
        // When
        Optional<User> foundUser = userRepository.findByEmail("local@user.com");

        // Then
        assertThat(foundUser).isPresent();
        assertThat(foundUser.get().getId()).isEqualTo(localUser.getId());
        assertThat(foundUser.get().getNickname()).isEqualTo("LocalUser");
    }

    @Test
    @DisplayName("findByEmail - 실패 (사용자 없음)")
    void findByEmail_fail_whenUserNotExists() {
        // When
        Optional<User> foundUser = userRepository.findByEmail("nonexistent@user.com");

        // Then
        assertThat(foundUser).isEmpty();
    }

    @Test
    @DisplayName("findByProviderAndProviderId - 성공 (사용자 존재)")
    void findByProviderAndProviderId_success_whenUserExists() {
        // When
        Optional<User> foundUser = userRepository.findByProviderAndProviderId("google", "google_123456");

        // Then
        assertThat(foundUser).isPresent();
        assertThat(foundUser.get().getId()).isEqualTo(googleUser.getId());
        assertThat(foundUser.get().getEmail()).isEqualTo("google@user.com");
    }

    @Test
    @DisplayName("findByProviderAndProviderId - 실패 (Provider 불일치)")
    void findByProviderAndProviderId_fail_wrongProvider() {
        // When
        Optional<User> foundUser = userRepository.findByProviderAndProviderId("kakao", "google_123456");

        // Then
        assertThat(foundUser).isEmpty();
    }

    @Test
    @DisplayName("findByProviderAndProviderId - 실패 (ProviderId 불일치)")
    void findByProviderAndProviderId_fail_wrongProviderId() {
        // When
        Optional<User> foundUser = userRepository.findByProviderAndProviderId("google", "999999");

        // Then
        assertThat(foundUser).isEmpty();
    }
}