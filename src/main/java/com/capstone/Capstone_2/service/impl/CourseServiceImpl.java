package com.capstone.Capstone_2.service.impl;

import com.capstone.Capstone_2.dto.CourseDto.*;
import com.capstone.Capstone_2.dto.CourseSearchDto;
import com.capstone.Capstone_2.dto.RecommendationDto;
import com.capstone.Capstone_2.entity.Category;
import com.capstone.Capstone_2.entity.Course;
import com.capstone.Capstone_2.entity.CourseSpot;
import com.capstone.Capstone_2.entity.CreatorProfile;
import com.capstone.Capstone_2.entity.ReviewState;
import com.capstone.Capstone_2.entity.User;
import com.capstone.Capstone_2.repository.*;
import com.capstone.Capstone_2.service.CourseService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict; // ✅ CacheEvict 임포트
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class CourseServiceImpl implements CourseService {

    private final CourseRepository courseRepo;
    private final CategoryRepository categoryRepo;
    private final CourseSpotRepository spotRepo;
    private final UserRepository userRepo;
    private final ObjectMapper objectMapper; // ✅ ObjectMapper 주입

    @Override
    public Detail create(CreateReq req, String creatorEmail) {
        User creatorUser = userRepo.findByEmail(creatorEmail)
                .orElseThrow(() -> new EntityNotFoundException("해당 이메일을 가진 사용자를 찾을 수 없습니다: " + creatorEmail));

        CreatorProfile creator = creatorUser.getCreatorProfile();
        if (creator == null) {
            throw new EntityNotFoundException("해당 사용자의 크리에이터 프로필이 존재하지 않습니다.");
        }

        // ✅ 웹 폼에서 넘어온 tagsString/metadataJson을 List/Map으로 변환
        processFormFields(req);

        Category category = null;
        // ✅ ( ) -> get...() 수정
        if (req.getCategoryId() != null) {
            category = categoryRepo.findById(req.getCategoryId())
                    .orElseThrow(() -> new EntityNotFoundException("카테고리를 찾을 수 없습니다."));
        }

        Course newCourse = Course.builder()
                .creator(creator)
                .category(category)
                // ✅ ( ) -> get...() 수정
                .title(req.getTitle())
                .summary(req.getSummary())
                .coverImageUrl(req.getCoverImageUrl())
                .regionCode(req.getRegionCode())
                .regionName(req.getRegionName())
                .durationMinutes(req.getDurationMinutes())
                .estimatedCost(req.getEstimatedCost())
                .tags(req.getTags() == null ? null : new HashSet<>(req.getTags())) // ✅ getTags()
                .metadata(req.getMetadata()) // ✅ getMetadata()
                .reviewState(ReviewState.DRAFT)
                .build();

        courseRepo.save(newCourse);

        // ✅ ( ) -> get...() 수정
        if (req.getSpots() != null && !req.getSpots().isEmpty()) {
            upsertSpots(newCourse, req.getSpots());
        }

        return toDetail(newCourse);
    }

    @Override
    public Detail update(UUID courseId, UpdateReq req, String currentUserEmail) {
        Course course = courseRepo.findById(courseId)
                .orElseThrow(() -> new EntityNotFoundException("코스를 찾을 수 없습니다."));

        if (!course.getCreator().getUser().getEmail().equals(currentUserEmail)) {
            throw new AccessDeniedException("이 코스를 수정할 권한이 없습니다.");
        }

        // ✅ 웹 폼에서 넘어온 tagsString/metadataJson을 List/Map으로 변환
        processFormFields(req);

        // ✅ ( ) -> get...() 수정
        if (req.getCategoryId() != null) {
            course.setCategory(categoryRepo.findById(req.getCategoryId())
                    .orElseThrow(() -> new EntityNotFoundException("카테고리를 찾을 수 없습니다.")));
        }
        if (req.getTitle() != null) course.setTitle(req.getTitle());
        if (req.getSummary() != null) course.setSummary(req.getSummary());
        if (req.getCoverImageUrl() != null) course.setCoverImageUrl(req.getCoverImageUrl());
        if (req.getRegionCode() != null) course.setRegionCode(req.getRegionCode());
        if (req.getRegionName() != null) course.setRegionName(req.getRegionName());
        if (req.getDurationMinutes() != null) course.setDurationMinutes(req.getDurationMinutes());
        if (req.getEstimatedCost() != null) course.setEstimatedCost(req.getEstimatedCost());
        if (req.getTags() != null) course.setTags(req.getTags() == null ? null : new HashSet<>(req.getTags()));
        if (req.getMetadata() != null) course.setMetadata(req.getMetadata());

        if (req.getSpots() != null) {
            spotRepo.deleteByCourse(course);
            upsertSpots(course, req.getSpots());
        }

        return toDetail(course);
    }

    @Override
    @CacheEvict(value = "popularCourses", allEntries = true) // ✅ 캐시 무효화 추가
    public void delete(UUID courseId, String currentUserEmail) {
        Course course = courseRepo.findById(courseId)
                .orElseThrow(() -> new EntityNotFoundException("코스를 찾을 수 없습니다."));

        if (!course.getCreator().getUser().getEmail().equals(currentUserEmail)) {
            throw new AccessDeniedException("이 코스를 삭제할 권한이 없습니다.");
        }

        // ✅ 외래 키 제약 조건 위반을 막기 위해 Spot을 먼저 삭제합니다.
        spotRepo.deleteByCourse(course);

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
    // 메소드 시그니처 변경: String q -> CourseSearchDto searchDto
    public Page<CourseSummary> search(CourseSearchDto searchDto, Pageable pageable) {
        // courseRepo.search() 대신 courseRepo.searchByFilter() 호출
        return courseRepo.searchByFilter(searchDto, pageable)
                .map(this::toSummary);
    }

    @Override
    public Detail submitForReview(UUID courseId, String currentUserEmail) {
        Course course = courseRepo.findById(courseId)
                .orElseThrow(() -> new EntityNotFoundException("코스를 찾을 수 없습니다."));

        if (!course.getCreator().getUser().getEmail().equals(currentUserEmail)) {
            throw new AccessDeniedException("이 코스를 제출할 권한이 없습니다.");
        }

        course.setReviewState(ReviewState.PENDING);
        return toDetail(course);
    }

    @Override
    @CacheEvict(value = "popularCourses", allEntries = true) // ✅ 캐시 무효화 추가
    public Detail approve(UUID courseId) {
        Course course = courseRepo.findById(courseId).orElseThrow(() -> new EntityNotFoundException("Course not found"));
        course.setReviewState(ReviewState.APPROVED);
        course.setPublishedAt(OffsetDateTime.now());
        return toDetail(course);
    }

    @Override
    @CacheEvict(value = "popularCourses", allEntries = true) // ✅ 캐시 무효화 추가
    public Detail reject(UUID courseId, String reason) {
        Course course = courseRepo.findById(courseId).orElseThrow(() -> new EntityNotFoundException("Course not found"));
        course.setReviewState(ReviewState.REJECTED);
        course.setRejectedReason(reason);
        return toDetail(course);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable("popularCourses")
    public Page<CourseSummary> getPopularCourses(Pageable pageable) {
        System.out.println("### DB에서 인기 코스를 조회합니다... ###");
        return courseRepo.findByOrderByLikeCountDesc(pageable)
                .map(this::toSummary);
    }

    // --- Helper Methods ---

    private void upsertSpots(Course c, List<SpotReq> spots) {
        List<CourseSpot> entities = spots.stream().map(s -> CourseSpot.builder()
                .course(c)
                // ✅ ( ) -> get...() 수정
                .orderNo(s.getOrderNo())
                .title(s.getTitle())
                .description(s.getDescription())
                .lat(s.getLat())
                .lng(s.getLng())
                .images(toJsonArray(s.getImages())) // ✅ getImages()
                .stayMinutes(s.getStayMinutes())
                .price(s.getPrice())
                .build()).toList();
        spotRepo.saveAll(entities);
    }

    // ✅ ObjectMapper를 사용하도록 수정
    private String toJsonArray(List<String> list) {
        if (list == null || list.isEmpty()) return "[]"; // null 대신 빈 JSON 배열 반환
        try {
            // ✅ ObjectMapper로 안전하게 JSON 직렬화
            return objectMapper.writeValueAsString(list);
        } catch (Exception e) {
            // 실제 환경에서는 로깅이 필요합니다.
            throw new RuntimeException("Failed to serialize images to JSON", e);
        }
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
                parseImages(s.getImages()), // ✅ 수정된 메서드 호출
                s.getStayMinutes(), s.getPrice()
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

    // ✅ ObjectMapper를 사용하도록 수정
    private List<String> parseImages(String json) {
        if (json == null || json.isBlank() || json.equals("[]")) return List.of();
        try {
            // ✅ ObjectMapper로 안전하게 JSON 역직렬화
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            // 로깅 후, 파싱 실패 시 빈 리스트 반환
            System.err.println("Failed to parse images JSON: " + json + "; Error: " + e.getMessage());
            return List.of();
        }
    }

    private void processFormFields(Object req) {
        // --- CreateReq 처리 ---
        if (req instanceof CreateReq r) {
            // tagsString 처리
            if ((r.getTags() == null || r.getTags().isEmpty()) && r.getTagsString() != null && !r.getTagsString().isBlank()) {
                r.setTags(Arrays.stream(r.getTagsString().split(","))
                        .map(String::trim)
                        .filter(s -> !s.isBlank())
                        .collect(Collectors.toList()));
            }
            // metadataJson 처리
            if ((r.getMetadata() == null || r.getMetadata().isEmpty()) && r.getMetadataJson() != null && !r.getMetadataJson().isBlank()) {
                try {
                    Map<String, Object> metadata = objectMapper.readValue(r.getMetadataJson(), new TypeReference<>() {});
                    r.setMetadata(metadata);
                } catch (Exception e) {
                    throw new IllegalArgumentException("Invalid metadata JSON format", e);
                }
            }
            // Spot 이미지 처리 (CreateReq에는 imagesInput이 없다고 가정, SpotReq의 images 필드 사용)
            if (r.getSpots() != null) {
                r.getSpots().forEach(spot -> {
                    // SpotReq에 imagesInput 필드가 있다면 여기서 파싱
                    // 예: if (spot.getImagesInput() != null) spot.setImages(parseCommaSeparatedString(spot.getImagesInput()));
                    // 여기서는 SpotReq의 images 필드가 이미 List<String>이라고 가정 (JS에서 처리했거나, 다른 방식 사용)
                    // 만약 HTML의 imagesInput(text)을 직접 받는다면 여기서 파싱해야 함.
                    // 임시 방편: SpotReq에 imagesInput 필드를 추가했다고 가정하고 파싱
                    // if (spot.getImagesInput() != null) { // SpotReq에 imagesInput:String 추가 필요
                    //     spot.setImages(parseCommaSeparatedString(spot.getImagesInput()));
                    // }
                });
            }

            // --- UpdateReq 처리 ---
        } else if (req instanceof UpdateReq r) {
            // tagsString 처리
            if ((r.getTags() == null || r.getTags().isEmpty()) && r.getTagsString() != null && !r.getTagsString().isBlank()) {
                r.setTags(Arrays.stream(r.getTagsString().split(","))
                        .map(String::trim)
                        .filter(s -> !s.isBlank())
                        .collect(Collectors.toList()));
            }
            // metadataJson 처리
            if ((r.getMetadata() == null || r.getMetadata().isEmpty()) && r.getMetadataJson() != null && !r.getMetadataJson().isBlank()) {
                try {
                    Map<String, Object> metadata = objectMapper.readValue(r.getMetadataJson(), new TypeReference<>() {});
                    r.setMetadata(metadata);
                } catch (Exception e) {
                    throw new IllegalArgumentException("Invalid metadata JSON format", e);
                }
            }
            // Spot 이미지 처리 (UpdateReq도 동일하게 처리)
            if (r.getSpots() != null) {
                r.getSpots().forEach(spot -> {
                    // HTML의 imagesInput 필드 (쉼표 구분 문자열)를 파싱하여 images (List<String>) 필드 설정
                    // 이를 위해 SpotReq DTO에 임시 필드(예: imagesInput)를 추가하거나,
                    // 여기서는 JavaScript에서 이미 List<String>으로 변환하여 보냈다고 가정합니다.
                    // 만약 HTML의 text input(spot-images-input)을 직접 처리해야 한다면,
                    // 해당 input의 name을 Controller에서 별도로 받아오거나 DTO에 임시 필드를 추가해야 합니다.

                    // 여기서는 별도 처리 없이 SpotReq의 images 필드가 List<String>으로 바인딩되었다고 가정.
                    // 만약 HTML의 imagesInput(text) 값을 파싱해야 한다면 아래 로직 필요:
                    // if (spot.getImagesInput() != null) { // SpotReq에 String imagesInput; 추가 필요
                    //     spot.setImages(parseCommaSeparatedString(spot.getImagesInput()));
                    // }
                });
            }
        }
    }

    // 쉼표 구분 문자열을 List<String>으로 파싱하는 헬퍼 메서드 (필요시 사용)
    private List<String> parseCommaSeparatedString(String input) {
        if (input == null || input.isBlank()) {
            return new ArrayList<>();
        }
        return Arrays.stream(input.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .collect(Collectors.toList());
    }


    @Override
    @Transactional(readOnly = true)
    public RecommendationDto getCourseRecommendations(UUID courseId) {
        Course sourceCourse = courseRepo.findById(courseId)
                .orElseThrow(() -> new EntityNotFoundException("Course not found"));

        Pageable limit = PageRequest.of(0, 5);

        List<CourseSummary> relatedByLikes = courseRepo.findRelatedCoursesByLikes(courseId, limit).stream()
                .map(this::toSummary).toList();

        List<CourseSummary> sameCategory = List.of();
        if (sourceCourse.getCategory() != null) {
            sameCategory = courseRepo.findByCategoryAndIdNotOrderByLikeCountDesc(sourceCourse.getCategory(), courseId, limit).stream()
                    .map(this::toSummary).toList();
        }

        List<CourseSummary> sameRegion = List.of();
        if (sourceCourse.getRegionCode() != null && !sourceCourse.getRegionCode().isBlank()) {
            sameRegion = courseRepo.findByRegionCodeAndIdNotOrderByLikeCountDesc(sourceCourse.getRegionCode(), courseId, limit).stream()
                    .map(this::toSummary).toList();
        }

        return RecommendationDto.builder()
                .relatedByLikes(relatedByLikes)
                .sameCategory(sameCategory)
                .sameRegion(sameRegion)
                .build();
    }
}

/*package com.capstone.Capstone_2.service.impl;

import com.capstone.Capstone_2.dto.CourseDto.*;
import com.capstone.Capstone_2.dto.RecommendationDto;
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
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
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

    @Override
    @Transactional(readOnly = true)
    @Cacheable("popularCourses")
    public Page<CourseSummary> getPopularCourses(Pageable pageable) {
        System.out.println("### DB에서 인기 코스를 조회합니다... ###"); // 캐시가 작동하는지 확인하기 위한 로그
        return courseRepo.findByOrderByLikeCountDesc(pageable)
                .map(this::toSummary);
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

    @Override
    @Transactional(readOnly = true)
    public RecommendationDto getCourseRecommendations(UUID courseId) {
        Course sourceCourse = courseRepo.findById(courseId)
                .orElseThrow(() -> new EntityNotFoundException("Course not found"));

        Pageable limit = PageRequest.of(0, 5); // 각 추천별 최대 5개

        // 1. 좋아요 기반 연관 추천
        List<CourseSummary> relatedByLikes = courseRepo.findRelatedCoursesByLikes(courseId, limit).stream()
                .map(this::toSummary).toList();

        // 2. 같은 카테고리 추천
        List<CourseSummary> sameCategory = List.of();
        if (sourceCourse.getCategory() != null) {
            sameCategory = courseRepo.findByCategoryAndIdNotOrderByLikeCountDesc(sourceCourse.getCategory(), courseId, limit).stream()
                    .map(this::toSummary).toList();
        }

        // 3. 같은 지역 추천
        List<CourseSummary> sameRegion = List.of();
        if (sourceCourse.getRegionCode() != null && !sourceCourse.getRegionCode().isBlank()) {
            sameRegion = courseRepo.findByRegionCodeAndIdNotOrderByLikeCountDesc(sourceCourse.getRegionCode(), courseId, limit).stream()
                    .map(this::toSummary).toList();
        }

        return RecommendationDto.builder()
                .relatedByLikes(relatedByLikes)
                .sameCategory(sameCategory)
                .sameRegion(sameRegion)
                .build();
    }
}
 */