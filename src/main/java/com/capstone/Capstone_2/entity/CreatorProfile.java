package com.capstone.Capstone_2.entity;

import com.capstone.Capstone_2.config.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "creator_profiles")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class CreatorProfile extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(columnDefinition = "uuid")
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(length = 60)
    private String displayName;

    @Column(columnDefinition = "TEXT")
    private String bio;

    @Column(precision = 4, scale = 2)
    private BigDecimal trustScore;


    @Column(precision = 3, scale = 2)
    private BigDecimal avgRating;


    private Integer totalSales = 0;
}
