package com.capstone.Capstone_2.entity;

import com.capstone.Capstone_2.config.common.BaseTimeEntity;
import com.capstone.Capstone_2.entity.CreatorProfile;
import io.hypersistence.utils.hibernate.type.array.ListArrayType;
import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.Type;

import java.time.OffsetDateTime;
import java.util.*;

@Entity
@Table(name = "courses")
@Getter
@Setter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class Course extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(columnDefinition = "uuid")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creator_id", nullable = false)
    private CreatorProfile creator;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String summary;

    private String coverImageUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @ColumnDefault("'DRAFT'")
    private ReviewState reviewState;

    @Column(columnDefinition = "TEXT")
    private String rejectedReason;

    @Column(length = 20)
    private String regionCode;

    @Column(length = 80)
    private String regionName;

    private Integer durationMinutes;

    private Integer estimatedCost;

    @Builder.Default
    @Column(nullable = false)
    private Integer likeCount = 0;

    @Builder.Default
    @Column(nullable = false)
    private Integer purchaseCount = 0;

    @ElementCollection
    @CollectionTable(name = "course_tags", joinColumns = @JoinColumn(name = "course_id"))
    @Column(name = "tag", length = 50, nullable = false)
    private Set<String> tags = new LinkedHashSet<>();

    @Type(JsonType.class)
    @Column(name = "metadata", columnDefinition = "jsonb")
    private Map<String, Object> metadata;

    private OffsetDateTime publishedAt;

    @OneToMany(mappedBy = "course", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orderNo asc")
    private List<CourseSpot> courseSpots;
}
