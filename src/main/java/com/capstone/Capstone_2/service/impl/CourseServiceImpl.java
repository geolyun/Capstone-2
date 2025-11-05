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
import com.capstone.Capstone_2.service.LikeService; // LikeService import 추가
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication; // Authentication import 추가
import org.springframework.security.core.context.SecurityContextHolder; // SecurityContextHolder import 추가
import org.springframework.security.core.userdetails.UserDetails; // UserDetails import 추가
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
    private final ObjectMapper objectMapper;
    private final LikeService likeService; // LikeService 주입
    private static final Logger logger = LoggerFactory.getLogger(CourseServiceImpl.class); // Logger 추가

    @Override
    @CacheEvict(value = {"popularCourses"}, allEntries = true) // 새 코스 생성 시 인기 코스 캐시 무효화
    public Detail create(CreateReq req, String creatorEmail) {
        User creatorUser = userRepo.findByEmail(creatorEmail)
                .orElseThrow(() -> new EntityNotFoundException("해당 이메일을 가진 사용자를 찾을 수 없습니다: " + creatorEmail));

        CreatorProfile creator = creatorUser.getCreatorProfile();
        if (creator == null) {
            throw new EntityNotFoundException("해당 사용자의 크리에이터 프로필이 존재하지 않습니다.");
        }

        // 폼 필드(tagsString, metadataJson) 처리
        processFormFields(req);

        Category category = null;
        if (req.getCategoryId() != null) {
            category = categoryRepo.findById(req.getCategoryId())
                    .orElseThrow(() -> new EntityNotFoundException("카테고리를 찾을 수 없습니다. ID: " + req.getCategoryId()));
        }

        Course newCourse = Course.builder()
                .creator(creator)
                .category(category)
                .title(req.getTitle())
                .summary(req.getSummary())
                .coverImageUrl(req.getCoverImageUrl())
                .regionCode(req.getRegionCode())
                .regionName(req.getRegionName())
                .durationMinutes(req.getDurationMinutes())
                .estimatedCost(req.getEstimatedCost())
                .tags(req.getTags() == null ? new HashSet<>() : new HashSet<>(req.getTags())) // null 대신 빈 Set 사용 고려
                // .metadata(req.getMetadata())
                .reviewState(ReviewState.DRAFT) // 최초 상태는 DRAFT
                // likeCount, purchaseCount는 Builder.Default로 0 초기화됨
                .build();

        courseRepo.save(newCourse);
        logger.info("New course saved with ID: {}", newCourse.getId());

        if (req.getSpots() != null && !req.getSpots().isEmpty()) {
            upsertSpots(newCourse, req.getSpots());
            logger.info("Spots added/updated for course ID: {}", newCourse.getId());
        }

        // 생성 직후에는 좋아요 상태가 false일 것이므로 null 전달 (또는 false로 직접 생성)
        return toDetail(newCourse, null);
    }

    @Override
    @CacheEvict(value = {"popularCourses"}, allEntries = true) // 코스 수정 시 인기 코스 캐시 무효화
    public Detail update(UUID courseId, UpdateReq req, String currentUserEmail) {
        Course course = courseRepo.findById(courseId)
                .orElseThrow(() -> new EntityNotFoundException("코스를 찾을 수 없습니다. ID: " + courseId));

        // 권한 확인: 코스 생성자와 현재 사용자가 동일한지 확인
        if (!course.getCreator().getUser().getEmail().equals(currentUserEmail)) {
            logger.warn("Access denied for updating course ID: {}. User: {}", courseId, currentUserEmail);
            throw new AccessDeniedException("이 코스를 수정할 권한이 없습니다.");
        }

        // 폼 필드 처리
        processFormFields(req);

        // 필드 업데이트 (null 체크 후 업데이트)
        if (req.getCategoryId() != null) {
            Category category = categoryRepo.findById(req.getCategoryId())
                    .orElseThrow(() -> new EntityNotFoundException("카테고리를 찾을 수 없습니다. ID: " + req.getCategoryId()));
            course.setCategory(category);
        }
        if (req.getTitle() != null) course.setTitle(req.getTitle());
        if (req.getSummary() != null) course.setSummary(req.getSummary());
        if (req.getCoverImageUrl() != null) course.setCoverImageUrl(req.getCoverImageUrl());
        if (req.getRegionCode() != null) course.setRegionCode(req.getRegionCode());
        if (req.getRegionName() != null) course.setRegionName(req.getRegionName());
        if (req.getDurationMinutes() != null) course.setDurationMinutes(req.getDurationMinutes());
        if (req.getEstimatedCost() != null) course.setEstimatedCost(req.getEstimatedCost());
        if (req.getTags() != null) course.setTags(new HashSet<>(req.getTags())); // null 대신 빈 Set 사용 고려
        // if (req.getMetadata() != null) course.setMetadata(req.getMetadata());
        // 스팟 업데이트 (기존 스팟 삭제 후 새로 추가)
        if (req.getSpots() != null) {
            logger.info("Deleting existing spots for course ID: {}", courseId);
            spotRepo.deleteByCourse(course); // 연관된 스팟 먼저 삭제
            if(!req.getSpots().isEmpty()){
                upsertSpots(course, req.getSpots());
                logger.info("New spots added/updated for course ID: {}", courseId);
            }
        }
        // courseRepo.save(course); // @Transactional 이므로 변경 감지로 자동 저장됨
        logger.info("Course updated successfully. ID: {}", courseId);

        // 업데이트 후 상세 정보 반환 시 현재 사용자 이메일 전달
        return toDetail(course, currentUserEmail);
    }

    @Override
    @CacheEvict(value = {"popularCourses"}, allEntries = true)
    public void delete(UUID courseId, String currentUserEmail) {
        Course course = courseRepo.findById(courseId)
                .orElseThrow(() -> new EntityNotFoundException("코스를 찾을 수 없습니다. ID: " + courseId));

        if (!course.getCreator().getUser().getEmail().equals(currentUserEmail)) {
            logger.warn("Access denied for deleting course ID: {}. User: {}", courseId, currentUserEmail);
            throw new AccessDeniedException("이 코스를 삭제할 권한이 없습니다.");
        }

        // 연관된 CourseSpot 먼저 삭제 (Cascade 설정이 없거나 확실히 하기 위해)
        // 주의: Like 등 다른 연관관계가 있다면 추가 처리 필요할 수 있음 (예: 좋아요 수 감소 등)
        logger.info("Deleting spots associated with course ID: {}", courseId);
        spotRepo.deleteByCourse(course);

        logger.info("Deleting course with ID: {}", courseId);
        courseRepo.delete(course);
    }

    @Override
    @Transactional(readOnly = true)
    public Detail get(UUID courseId, String currentUserEmail) {
        Course course = courseRepo.findById(courseId)
                .orElseThrow(() -> new EntityNotFoundException("코스를 찾을 수 없습니다. ID: " + courseId));


        return toDetail(course, currentUserEmail);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CourseSummary> search(CourseSearchDto searchDto, Pageable pageable) {
        // QueryDSL 구현체 사용 (CourseRepositoryImpl)
        logger.debug("Searching courses with DTO: {} and Pageable: {}", searchDto, pageable);
        return courseRepo.searchByFilter(searchDto, pageable)
                .map(this::toSummary); // Course -> CourseSummary 변환
    }

    @Override
    @CacheEvict(value = {"popularCourses"}, allEntries = true) // 상태 변경 시 캐시 무효화
    public Detail submitForReview(UUID courseId, String currentUserEmail) {
        Course course = courseRepo.findById(courseId)
                .orElseThrow(() -> new EntityNotFoundException("코스를 찾을 수 없습니다. ID: " + courseId));

        if (!course.getCreator().getUser().getEmail().equals(currentUserEmail)) {
            logger.warn("Access denied for submitting course ID: {}. User: {}", courseId, currentUserEmail);
            throw new AccessDeniedException("이 코스를 제출할 권한이 없습니다.");
        }
        // DRAFT 상태일 때만 PENDING으로 변경 가능하도록 조건 추가 가능
        if (course.getReviewState() != ReviewState.DRAFT && course.getReviewState() != ReviewState.REJECTED) {
            logger.warn("Course {} is not in DRAFT or REJECTED state, cannot submit for review.", courseId);
            throw new IllegalStateException("이미 검토 중이거나 승인된 코스입니다.");
        }

        course.setReviewState(ReviewState.PENDING);
        logger.info("Course {} submitted for review.", courseId);
        // courseRepo.save(course); // 자동 저장
        return toDetail(course, currentUserEmail);
    }

    @Override
    @CacheEvict(value = {"popularCourses"}, allEntries = true)
    // @PreAuthorize("hasRole('ADMIN')") // 서비스 레벨 권한 검사 (선택 사항)
    public Detail approve(UUID courseId) {
        Course course = courseRepo.findById(courseId)
                .orElseThrow(() -> new EntityNotFoundException("코스를 찾을 수 없습니다. ID: " + courseId));

        // PENDING 상태일 때만 승인 가능하도록 조건 추가 가능
        if (course.getReviewState() != ReviewState.PENDING) {
            logger.warn("Course {} is not in PENDING state, cannot approve.", courseId);
            throw new IllegalStateException("검토 중인 코스만 승인할 수 있습니다.");
        }

        course.setReviewState(ReviewState.APPROVED);
        course.setPublishedAt(OffsetDateTime.now()); // 승인 시 게시 시간 설정
        course.setRejectedReason(null); // 거절 사유 초기화
        logger.info("Course {} approved.", courseId);
        // courseRepo.save(course); // 자동 저장
        // 관리자 액션이므로 현재 사용자 컨텍스트가 없을 수 있음
        return toDetail(course, getCurrentUserEmailForAdminActions());
    }

    @Override
    @CacheEvict(value = {"popularCourses"}, allEntries = true)
    // @PreAuthorize("hasRole('ADMIN')") // 서비스 레벨 권한 검사 (선택 사항)
    public Detail reject(UUID courseId, String reason) {
        Course course = courseRepo.findById(courseId)
                .orElseThrow(() -> new EntityNotFoundException("코스를 찾을 수 없습니다. ID: " + courseId));

        // PENDING 상태일 때만 거절 가능하도록 조건 추가 가능
        if (course.getReviewState() != ReviewState.PENDING) {
            logger.warn("Course {} is not in PENDING state, cannot reject.", courseId);
            throw new IllegalStateException("검토 중인 코스만 거절할 수 있습니다.");
        }
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("거절 사유는 필수입니다.");
        }

        course.setReviewState(ReviewState.REJECTED);
        course.setRejectedReason(reason);
        course.setPublishedAt(null); // 게시 시간 초기화
        logger.info("Course {} rejected with reason: {}", courseId, reason);
        // courseRepo.save(course); // 자동 저장
        // 관리자 액션
        return toDetail(course, getCurrentUserEmailForAdminActions());
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable("popularCourses") // 메소드 결과를 캐싱 (key는 pageable 파라미터 기반 자동 생성)
    public Page<CourseSummary> getPopularCourses(Pageable pageable) {
        logger.info("Fetching popular courses from DB for pageable: {}", pageable); // 캐시 미스 시 로그 출력
        // findByOrderByLikeCountDesc는 모든 상태의 코스를 가져올 수 있음
        // 필요하다면 APPROVED 상태만 가져오도록 쿼리 수정 또는 필터링 필요
        // 예: Specification 사용 또는 QueryDSL 수정
        return courseRepo.findByOrderByLikeCountDesc(pageable) // likeCount 기준 내림차순
                .map(this::toSummary);
    }

    @Override
    @Transactional(readOnly = true)
    public RecommendationDto getCourseRecommendations(UUID courseId) {
        Course sourceCourse = courseRepo.findById(courseId)
                .orElseThrow(() -> new EntityNotFoundException("추천 기준 코스를 찾을 수 없습니다. ID: " + courseId));

        Pageable limit = PageRequest.of(0, 5); // 각 추천별 최대 5개

        // 1. 좋아요 기반 연관 추천 (동일 코스 제외)
        List<CourseSummary> relatedByLikes = courseRepo.findRelatedCoursesByLikes(courseId, limit).stream()
                .map(this::toSummary).toList();

        // 2. 같은 카테고리 인기 코스 (동일 코스 제외)
        List<CourseSummary> sameCategory = List.of();
        if (sourceCourse.getCategory() != null) {
            sameCategory = courseRepo.findByCategoryAndIdNotOrderByLikeCountDesc(sourceCourse.getCategory(), courseId, limit).stream()
                    .map(this::toSummary).toList();
        }

        // 3. 같은 지역 인기 코스 (동일 코스 제외)
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


    // --- Helper Methods ---

    /**
     * CourseSpot 리스트를 저장/업데이트합니다. (기존 데이터 삭제 후 새로 삽입 방식)
     */
    private void upsertSpots(Course course, List<SpotReq> spots) {
        List<CourseSpot> entities = spots.stream().map(spotDto -> CourseSpot.builder()
                .course(course) // Course 엔티티와 연관관계 설정
                .orderNo(spotDto.getOrderNo())
                .title(spotDto.getTitle())
                .description(spotDto.getDescription())
                .lat(spotDto.getLat())
                .lng(spotDto.getLng())
                .images(toJsonArray(spotDto.getImages())) // List<String> -> JSON String 변환
                .stayMinutes(spotDto.getStayMinutes())
                .price(spotDto.getPrice())
                .build()).toList();
        spotRepo.saveAll(entities); // 일괄 저장
    }

    /**
     * 문자열 리스트를 JSON 배열 문자열로 변환합니다.
     */
    private String toJsonArray(List<String> list) {
        if (list == null || list.isEmpty()) return "[]"; // null 대신 빈 JSON 배열 반환
        try {
            return objectMapper.writeValueAsString(list);
        } catch (Exception e) {
            logger.error("Failed to serialize list to JSON array: {}", list, e);
            throw new RuntimeException("이미지 목록을 JSON으로 변환하는 데 실패했습니다.", e); // 예외 전환
        }
    }

    /**
     * JSON 배열 문자열을 문자열 리스트로 변환합니다.
     */
    private List<String> parseImages(String json) {
        if (json == null || json.isBlank() || json.equals("[]")) return List.of(); // 빈 리스트 반환
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            logger.error("Failed to parse images JSON string: {}", json, e);
            return List.of(); // 파싱 실패 시 빈 리스트 반환
        }
    }

    /**
     * Course 엔티티를 CourseSummary DTO로 변환합니다.
     */
    private CourseSummary toSummary(Course c) {
        return new CourseSummary(
                c.getId(), c.getTitle(), c.getSummary(), c.getCoverImageUrl(),
                c.getRegionName(), c.getDurationMinutes(), c.getEstimatedCost(),
                c.getLikeCount(), c.getPurchaseCount(), c.getReviewState(), c.getCreatedAt()
        );
    }

    /**
     * Course 엔티티를 Detail DTO로 변환합니다. 현재 로그인 사용자의 좋아요 상태를 포함합니다.
     */
    private Detail toDetail(Course c, String currentUserEmail) {
        List<CourseSpot> spots = spotRepo.findByCourseOrderByOrderNoAsc(c); // 정렬된 스팟 조회
        List<SpotRes> spotResList = spots.stream().map(s -> new SpotRes(
                s.getOrderNo(), s.getTitle(), s.getDescription(), s.getLat(), s.getLng(),
                parseImages(s.getImages()), // JSON String -> List<String> 변환
                s.getStayMinutes(), s.getPrice()
        )).toList();

        // 현재 사용자의 좋아요 상태 확인
        boolean isLiked = false;
        if (currentUserEmail != null) {
            try {
                isLiked = likeService.isCourseLikedByUser(c.getId(), currentUserEmail);
            } catch (Exception e) {
                // 사용자를 찾을 수 없거나 다른 예외 발생 시 로깅하고 false 처리
                logger.error("Error checking like status for user {} and course {}: {}", currentUserEmail, c.getId(), e.getMessage());
            }
        }

        return new Detail(
                c.getId(),
                c.getCreator().getId(), // CreatorProfile ID
                c.getCreator().getDisplayName(), // Creator 표시 이름
                c.getCategory() == null ? null : c.getCategory().getSlug(), // Category 슬러그
                c.getTitle(), c.getSummary(), c.getCoverImageUrl(),
                c.getRegionCode(), c.getRegionName(), c.getDurationMinutes(), c.getEstimatedCost(),
                c.getTags() == null ? List.of() : new ArrayList<>(c.getTags()), // Set -> List 변환
                // c.getMetadata(),
                c.getLikeCount(), c.getPurchaseCount(), c.getReviewState(),
                c.getPublishedAt(),
                spotResList,
                isLiked, // 좋아요 상태 추가
                c.getCreatedAt()
        );
    }

    /**
     * CreateReq 또는 UpdateReq 객체에서 tagsString과 metadataJson 필드를 처리합니다.
     */
    private void processFormFields(Object reqDto) {
        // CreateReq 처리
        if (reqDto instanceof CreateReq req) {
            // tagsString 처리 (tags 리스트가 비어있고 tagsString이 있을 때만)
            if ((req.getTags() == null || req.getTags().isEmpty()) && req.getTagsString() != null && !req.getTagsString().isBlank()) {
                req.setTags(parseCommaSeparatedString(req.getTagsString()));
            }

            // CreateReq의 SpotReq 리스트 내 이미지 처리 (만약 SpotReq에 imagesInput이 있다면)
            // if (req.getSpots() != null) {
            //     req.getSpots().forEach(spot -> {
            //         if (spot.getImagesInput() != null) { // SpotReq에 String imagesInput; 필드 추가 가정
            //              spot.setImages(parseCommaSeparatedString(spot.getImagesInput()));
            //         }
            //     });
            // }

            // UpdateReq 처리
        } else if (reqDto instanceof UpdateReq req) {
            // tagsString 처리
            if ((req.getTags() == null || req.getTags().isEmpty()) && req.getTagsString() != null && !req.getTagsString().isBlank()) {
                req.setTags(parseCommaSeparatedString(req.getTagsString()));
            }

            // UpdateReq의 SpotReq 리스트 내 이미지 처리 (CreateReq와 동일 로직 적용 가능)
            // if (req.getSpots() != null) {
            //    req.getSpots().forEach(spot -> {
            //        if (spot.getImagesInput() != null) {
            //             spot.setImages(parseCommaSeparatedString(spot.getImagesInput()));
            //        }
            //    });
            // }
        }
    }

    /**
     * 현재 Security Context에서 인증된 사용자의 이메일(username)을 가져옵니다.
     * 없으면 null을 반환합니다.
     */
    private String getCurrentUserEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() && !"anonymousUser".equals(authentication.getPrincipal())) {
            Object principal = authentication.getPrincipal();
            if (principal instanceof UserDetails) {
                return ((UserDetails) principal).getUsername();
            } else if (principal instanceof String) {
                return (String) principal;
            }
            // UserPrincipal 타입 등 다른 Principal 타입 처리 추가 가능
        }
        return null;
    }

    /**
     * 관리자 액션 시 현재 사용자 이메일을 가져옵니다. (로깅 등 필요시 사용)
     */
    private String getCurrentUserEmailForAdminActions() {
        // getCurrentUserEmail() 재사용 가능
        return getCurrentUserEmail();
    }

    /**
     * 쉼표로 구분된 문자열을 파싱하여 List<String>으로 반환합니다.
     */
    private List<String> parseCommaSeparatedString(String input) {
        if (input == null || input.isBlank()) {
            return new ArrayList<>();
        }
        return Arrays.stream(input.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .collect(Collectors.toList());
    }
}