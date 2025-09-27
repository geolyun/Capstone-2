package com.capstone.Capstone_2.service.impl;

import com.capstone.Capstone_2.entity.Category;
import com.capstone.Capstone_2.entity.Course;
import com.capstone.Capstone_2.entity.CreatorProfile;
import com.capstone.Capstone_2.entity.CourseSpot;
import com.capstone.Capstone_2.entity.ReviewState;
import com.capstone.Capstone_2.dto.CourseDto.*;
import com.capstone.Capstone_2.repository.*;
import com.capstone.Capstone_2.service.CourseService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.access.AccessDeniedException;


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


    @Override
    public Detail create(CreateReq req, UUID creatorUserId) {
        CreatorProfile creator = creatorRepo.findByUserId(creatorUserId)
                .orElseThrow(() -> new EntityNotFoundException("creator not found"));
        Category category = null;
        if (req.categoryId() != null) {
            category = categoryRepo.findById(req.categoryId())
                    .orElseThrow(() -> new EntityNotFoundException("category not found"));
        }
        Course c = Course.builder()
                .creator(creator)
                .category(category)
                .title(req.title())
                .summary(req.summary())
                .coverImageUrl(req.coverImageUrl())
                .regionCode(req.regionCode())
                .regionName(req.regionName())
                .durationMinutes(req.durationMinutes())
                .estimatedCost(req.estimatedCost())
                .metadata(req.metadata())
                .reviewState(ReviewState.DRAFT)
                .build();
        courseRepo.save(c);

        if (req.tags() != null) {
            c.getTags().addAll(normalizeTags(req.tags()));
        }
        courseRepo.save(c);


        upsertSpots(c, req.spots());
        return toDetail(c);
    }


    @Override
    public Detail update(UUID courseId, UpdateReq req, UUID currentUserId) {
        Course c = courseRepo.findById(courseId)
                .orElseThrow(() -> new EntityNotFoundException("course not found"));

        // 본인 확인
        if (!c.getCreator().getUser().getId().equals(currentUserId)) {
            throw new AccessDeniedException("You do not have permission to update this course.");
        }

        if (req.categoryId() != null) {
            c.setCategory(categoryRepo.findById(req.categoryId())
                    .orElseThrow(() -> new EntityNotFoundException("category not found")));
        }

        if (req.tags() != null) {
            c.getTags().clear();
            c.getTags().addAll(normalizeTags(req.tags()));
        }

        if (req.title() != null) c.setTitle(req.title());
        if (req.summary() != null) c.setSummary(req.summary());
        if (req.coverImageUrl() != null) c.setCoverImageUrl(req.coverImageUrl());
        if (req.regionCode() != null) c.setRegionCode(req.regionCode());
        if (req.regionName() != null) c.setRegionName(req.regionName());
        if (req.durationMinutes() != null) c.setDurationMinutes(req.durationMinutes());
        if (req.estimatedCost() != null) c.setEstimatedCost(req.estimatedCost());
        if (req.tags() != null) {                 // null이면 '태그 변경 없음'
            c.getTags().clear();                  // 비어있는 리스트가 들어오면 '전부 제거'가 됨
            c.getTags().addAll(normalizeTags(req.tags()));  // 중복/공백 정리해서 추가
        }
        if (req.metadata() != null) c.setMetadata(req.metadata());


        if (req.spots() != null) {
            spotRepo.deleteByCourse(c);
            upsertSpots(c, req.spots());
        }
        return toDetail(c);
    }


    @Override
    public void delete(UUID courseId, UUID currentUserId) {
        Course c = courseRepo.findById(courseId)
                .orElseThrow(() -> new EntityNotFoundException("course not found"));

        // 본인 확인
        if (!c.getCreator().getUser().getId().equals(currentUserId)) {
            throw new AccessDeniedException("You do not have permission to delete this course.");
        }

        courseRepo.delete(c);
    }


    @Override @Transactional(readOnly = true)
    public Detail get(UUID courseId) {
        Course c = courseRepo.findById(courseId)
                .orElseThrow(() -> new EntityNotFoundException("course not found"));
        return toDetail(c);
    }


    @Override @Transactional(readOnly = true)
    public Page<CourseSummary> search(String q, Pageable pageable) {
        return courseRepo.search(ReviewState.APPROVED, q, pageable)
                .map(this::toSummary);
    }


    @Override
    public Detail submitForReview(UUID courseId, UUID currentUserId) {
        Course c = courseRepo.findById(courseId).orElseThrow(() -> new EntityNotFoundException("Course not found"));

        // 본인 확인
        if (!c.getCreator().getUser().getId().equals(currentUserId)) {
            throw new AccessDeniedException("You are not the owner of this course.");
        }

        c.setReviewState(ReviewState.PENDING);
        return toDetail(c);
    }

    @Override
    public Detail approve(UUID courseId) {
        Course c = courseRepo.findById(courseId).orElseThrow();
        c.setReviewState(ReviewState.APPROVED);
        c.setPublishedAt(java.time.OffsetDateTime.now());
        return toDetail(c);
    }


    @Override
    public Detail reject(UUID courseId, String reason) {
        Course c = courseRepo.findById(courseId).orElseThrow();
        c.setReviewState(ReviewState.REJECTED);
        c.setRejectedReason(reason);
        return toDetail(c);
    }


    private void upsertSpots(Course c, List<SpotReq> spots) {
        List<CourseSpot> entities = spots.stream().map(s -> CourseSpot.builder()
                .course(c)
                .orderNo(s.orderNo())
                .title(s.title())
                .description(s.description())
                .lat(s.lat())
                .lng(s.lng())
                .images(s.images() == null ? null : toJsonArray(s.images()))
                .stayMinutes(s.stayMinutes())
                .price(s.price())
                .build()).toList();
        spotRepo.saveAll(entities);
    }


    private String toJsonArray(List<String> list) {
        return list == null ? null : list.stream()
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
                c.getTags() == null ? List.of() : new ArrayList<>(c.getTags()),  // ← Set → List
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

    private Set<String> normalizeTags(List<String> src) {
        if (src == null) return Set.of();
        Set<String> out = new LinkedHashSet<>();
        for (String s : src) {
            if (s == null) continue;
            String t = s.trim();
            if (!t.isEmpty()) out.add(t);
        }
        return out;
    }


}