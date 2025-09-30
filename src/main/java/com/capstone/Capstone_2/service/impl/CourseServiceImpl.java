package com.capstone.Capstone_2.service.impl;

import com.capstone.Capstone_2.dto.CourseDto.*;
import com.capstone.Capstone_2.entity.Category;
import com.capstone.Capstone_2.entity.Course;
import com.capstone.Capstone_2.entity.CourseSpot;
import com.capstone.Capstone_2.entity.CreatorProfile;
import com.capstone.Capstone_2.entity.ReviewState;
import com.capstone.Capstone_2.entity.User;
import com.capstone.Capstone_2.repository.*;
import com.capstone.Capstone_2.service.CourseService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class CourseServiceImpl implements CourseService {

    private final CourseRepository courseRepo;
    private final CreatorProfileRepository creatorRepo;
    private final CategoryRepository categoryRepo;
    private final CourseSpotRepository spotRepo;
    private final UserRepository userRepo; // ✅ User 조회를 위해 추가

    @Override
    public Detail create(CreateReq req, String creatorEmail) {
        // ✅ 1. 이메일로 User를 찾고, 거기서 CreatorProfile을 가져옵니다.
        User creatorUser = userRepo.findByEmail(creatorEmail)
                .orElseThrow(() -> new EntityNotFoundException("해당 이메일을 가진 사용자를 찾을 수 없습니다: " + creatorEmail));

        CreatorProfile creator = creatorUser.getCreatorProfile();
        if (creator == null) {
            throw new EntityNotFoundException("해당 사용자의 크리에이터 프로필이 존재하지 않습니다.");
        }

        Category category = null;
        if (req.categoryId() != null) {
            category = categoryRepo.findById(req.categoryId())
                    .orElseThrow(() -> new EntityNotFoundException("카테고리를 찾을 수 없습니다."));
        }

        Course newCourse = Course.builder()
                .creator(creator)
                .category(category)
                .title(req.title())
                .summary(req.summary())
                .coverImageUrl(req.coverImageUrl())
                .regionCode(req.regionCode())
                .regionName(req.regionName())
                .durationMinutes(req.durationMinutes())
                .estimatedCost(req.estimatedCost())
                .tags(req.tags() == null ? null : new HashSet<>(req.tags()))
                .metadata(req.metadata())
                .reviewState(ReviewState.DRAFT)
                .build();

        courseRepo.save(newCourse);

        if (req.spots() != null && !req.spots().isEmpty()) {
            upsertSpots(newCourse, req.spots());
        }

        return toDetail(newCourse);
    }

    @Override
    public Detail update(UUID courseId, UpdateReq req, String currentUserEmail) {
        Course course = courseRepo.findById(courseId)
                .orElseThrow(() -> new EntityNotFoundException("코스를 찾을 수 없습니다."));

        // ✅ 2. 이메일로 본인 확인
        if (!course.getCreator().getUser().getEmail().equals(currentUserEmail)) {
            throw new AccessDeniedException("이 코스를 수정할 권한이 없습니다.");
        }

        if (req.categoryId() != null) {
            course.setCategory(categoryRepo.findById(req.categoryId())
                    .orElseThrow(() -> new EntityNotFoundException("카테고리를 찾을 수 없습니다.")));
        }
        if (req.title() != null) course.setTitle(req.title());
        if (req.summary() != null) course.setSummary(req.summary());
        if (req.coverImageUrl() != null) course.setCoverImageUrl(req.coverImageUrl());
        if (req.regionCode() != null) course.setRegionCode(req.regionCode());
        if (req.regionName() != null) course.setRegionName(req.regionName());
        if (req.durationMinutes() != null) course.setDurationMinutes(req.durationMinutes());
        if (req.estimatedCost() != null) course.setEstimatedCost(req.estimatedCost());
        if (req.tags() != null) course.setTags(req.tags() == null ? null : new HashSet<>(req.tags()));
        if (req.metadata() != null) course.setMetadata(req.metadata());

        if (req.spots() != null) {
            spotRepo.deleteByCourse(course);
            upsertSpots(course, req.spots());
        }

        return toDetail(course);
    }

    @Override
    public void delete(UUID courseId, String currentUserEmail) {
        Course course = courseRepo.findById(courseId)
                .orElseThrow(() -> new EntityNotFoundException("코스를 찾을 수 없습니다."));

        // ✅ 이메일로 본인 확인
        if (!course.getCreator().getUser().getEmail().equals(currentUserEmail)) {
            throw new AccessDeniedException("이 코스를 삭제할 권한이 없습니다.");
        }

        courseRepo.delete(course);
    }

    @Override
    @Transactional(readOnly = true)
    public Detail get(UUID courseId) {
        Course course = courseRepo.findById(courseId)
                .orElseThrow(() -> new EntityNotFoundException("코스를 찾을 수 없습니다."));
        return toDetail(course);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CourseSummary> search(String q, Pageable pageable) {
        return courseRepo.search(ReviewState.APPROVED, q, pageable)
                .map(this::toSummary);
    }

    @Override
    public Detail submitForReview(UUID courseId, String currentUserEmail) {
        Course course = courseRepo.findById(courseId)
                .orElseThrow(() -> new EntityNotFoundException("코스를 찾을 수 없습니다."));

        // ✅ 이메일로 본인 확인
        if (!course.getCreator().getUser().getEmail().equals(currentUserEmail)) {
            throw new AccessDeniedException("이 코스를 제출할 권한이 없습니다.");
        }

        course.setReviewState(ReviewState.PENDING);
        return toDetail(course);
    }

    @Override
    public Detail approve(UUID courseId) {
        Course course = courseRepo.findById(courseId).orElseThrow(() -> new EntityNotFoundException("Course not found"));
        course.setReviewState(ReviewState.APPROVED);
        course.setPublishedAt(OffsetDateTime.now());
        return toDetail(course);
    }

    @Override
    public Detail reject(UUID courseId, String reason) {
        Course course = courseRepo.findById(courseId).orElseThrow(() -> new EntityNotFoundException("Course not found"));
        course.setReviewState(ReviewState.REJECTED);
        course.setRejectedReason(reason);
        return toDetail(course);
    }

    // --- Helper Methods ---

    private void upsertSpots(Course c, List<SpotReq> spots) {
        List<CourseSpot> entities = spots.stream().map(s -> CourseSpot.builder()
                .course(c)
                .orderNo(s.orderNo())
                .title(s.title())
                .description(s.description())
                .lat(s.lat()) // DTO의 lat(BigDecimal)를 그대로 사용
                .lng(s.lng()) // DTO의 lng(BigDecimal)를 그대로 사용
                .images(toJsonArray(s.images()))
                .stayMinutes(s.stayMinutes())
                .price(s.price())
                .build()).toList();
        spotRepo.saveAll(entities);
    }

    private String toJsonArray(List<String> list) {
        if (list == null || list.isEmpty()) return null;
        // 실제 운영 환경에서는 Jackson과 같은 라이브러리 사용을 권장합니다.
        return list.stream()
                .map(s -> "\"" + s.replace("\"", "\\\"") + "\"")
                .collect(Collectors.joining(",", "[", "]"));
    }

    private CourseSummary toSummary(Course c) {
        return new CourseSummary(
                c.getId(), c.getTitle(), c.getSummary(), c.getCoverImageUrl(),
                c.getRegionName(), c.getDurationMinutes(), c.getEstimatedCost(),
                c.getLikeCount(), c.getPurchaseCount(), c.getReviewState()
        );
    }

    private Detail toDetail(Course c) {
        List<CourseSpot> spots = spotRepo.findByCourseOrderByOrderNoAsc(c);
        var spotRes = spots.stream().map(s -> new SpotRes(
                s.getOrderNo(), s.getTitle(), s.getDescription(), s.getLat(), s.getLng(),
                parseImages(s.getImages()), s.getStayMinutes(), s.getPrice()
        )).toList();
        return new Detail(
                c.getId(),
                c.getCreator().getId(),
                c.getCreator().getDisplayName(),
                c.getCategory() == null ? null : c.getCategory().getSlug(),
                c.getTitle(), c.getSummary(), c.getCoverImageUrl(),
                c.getRegionCode(), c.getRegionName(), c.getDurationMinutes(), c.getEstimatedCost(),
                c.getTags() == null ? List.of() : new ArrayList<>(c.getTags()),
                c.getMetadata(), c.getLikeCount(), c.getPurchaseCount(), c.getReviewState(),
                c.getPublishedAt(), spotRes
        );
    }

    private List<String> parseImages(String json) {
        if (json == null || json.isBlank()) return List.of();
        // 실제 운영 환경에서는 Jackson과 같은 라이브러리 사용을 권장합니다.
        return Arrays.stream(json.replace("[", "").replace("]", "").split(","))
                .map(s -> s.trim().replace("\"", ""))
                .filter(s -> !s.isBlank())
                .toList();
    }
}