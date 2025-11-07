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
    private UUID id; // 이 부분은 수정할 필요 없습니다.

    @ManyToOne(fetch = FetchType.LAZY)
    // ✅ 수정: columnDefinition = "binary(16)" 추가
    @JoinColumn(name = "reporter_id", columnDefinition = "binary(16)")
    private User reporter; // 신고한 사용자

    @ManyToOne(fetch = FetchType.LAZY)
    // ✅ 수정: columnDefinition = "binary(16)" 추가
    @JoinColumn(name = "course_id", columnDefinition = "binary(16)")
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