package com.capstone.Capstone_2.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "reports")
public class Report {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reporter_id")
    private User reporter; // 신고한 사용자

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id")
    private Course reportedCourse; // 신고된 코스

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReportReason reason; // 신고 사유

    @Column(columnDefinition = "TEXT")
    private String description; // 상세 설명

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReportStatus status = ReportStatus.PENDING; // 처리 상태

    @CreationTimestamp
    private OffsetDateTime createdAt;

    // 신고 사유 Enum
    public enum ReportReason {
        SPAM, INAPPROPRIATE_CONTENT, COPYRIGHT_VIOLATION, ETC
    }

    // 신고 처리 상태 Enum
    public enum ReportStatus {
        PENDING, RESOLVED
    }
}