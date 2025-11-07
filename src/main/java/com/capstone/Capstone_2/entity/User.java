package com.capstone.Capstone_2.entity;

import com.capstone.Capstone_2.config.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;

import java.util.UUID;

@Entity
@Table(name = "users")
@Getter
@Setter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class User extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(unique = true, nullable = false)
    private String email;

    private String passwordHash;

    @Column(nullable = false)
    private String provider = "local";

    private String providerId;

    @Column(unique = true, nullable = false, length = 40)
    private String nickname;

    private String avatarUrl;

    @Enumerated(EnumType.STRING) // ✅ 이 어노테이션을 추가합니다.
    @Column(nullable = false)
    @Builder.Default
    private UserRole role = UserRole.USER;

    @Column(nullable = false)
    private String status = "active";

    @OneToOne(mappedBy = "user", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private CreatorProfile creatorProfile;
}
