/* package com.capstone.Capstone_2.service.impl;

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
import com.capstone.Capstone_2.service.LikeService;
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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
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
    private final UserRepository userRepo;
    private final ObjectMapper objectMapper;
    private final LikeService likeService;
    private static final Logger logger = LoggerFactory.getLogger(CourseServiceImpl.class);

    @Override
    @CacheEvict(value = {"popularCourses"}, allEntries = true)
    public Detail create(CreateReq req, String creatorEmail) {
        User creatorUser = userRepo.findByEmail(creatorEmail)
                .orElseThrow(() -> new EntityNotFoundException("해당 이메일을 가진 사용자를 찾을 수 없습니다: " + creatorEmail));

        CreatorProfile creator = creatorUser.getCreatorProfile();
        if (creator == null) {
            throw new EntityNotFoundException("해당 사용자의 크리에이터 프로필이 존재하지 않습니다.");
        }

        processFormFields(req);

        Category category = null;
        if (req.getCategoryId() != null) {
            category = categoryRepo.findById(req.getCategoryId())
                    .orElseThrow(() -> new EntityNotFoundException("카테고리를 찾을 수 없습니다. ID: " + req.getCategoryId()));
        }

        // 1. 코스 객체 생성 (아직 저장 전)
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
                .tags(req.getTags() == null ? new HashSet<>() : new HashSet<>(req.getTags()))
                .reviewState(ReviewState.DRAFT)
                .courseSpots(new ArrayList<>()) // ✅ 빈 리스트로 초기화
                .build();

        // 2. 스팟 리스트 생성 및 코스에 추가 (Cascade 설정 덕분에 코스 저장 시 같이 저장됨)
        if (req.getSpots() != null && !req.getSpots().isEmpty()) {
            List<CourseSpot> spots = req.getSpots().stream().map(spotDto -> CourseSpot.builder()
                    .course(newCourse) // 연관관계 설정
                    .orderNo(spotDto.getOrderNo())
                    .title(spotDto.getTitle())
                    .description(spotDto.getDescription())
                    .lat(spotDto.getLat())
                    .lng(spotDto.getLng())
                    .images(toJsonArray(spotDto.getImages()))
                    .stayMinutes(spotDto.getStayMinutes())
                    .price(spotDto.getPrice())
                    .build()).toList();

            newCourse.getCourseSpots().addAll(spots); // ✅ 리스트에 추가
        }

        // 3. 코스 저장 (스팟들도 자동으로 함께 저장됨)
        courseRepo.save(newCourse);
        logger.info("New course created with ID: {}", newCourse.getId());

        return toDetail(newCourse, null);
    }

    @Override
    @CacheEvict(value = {"popularCourses"}, allEntries = true)
    public Detail update(UUID courseId, UpdateReq req, String currentUserEmail) {
        Course course = courseRepo.findById(courseId)
                .orElseThrow(() -> new EntityNotFoundException("코스를 찾을 수 없습니다. ID: " + courseId));

        if (!course.getCreator().getUser().getEmail().equals(currentUserEmail)) {
            logger.warn("Access denied for updating course ID: {}. User: {}", courseId, currentUserEmail);
            throw new AccessDeniedException("이 코스를 수정할 권한이 없습니다.");
        }

        processFormFields(req);

        // 필드 업데이트
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
        if (req.getTags() != null) course.setTags(new HashSet<>(req.getTags()));

        // ✅ 스팟 업데이트 (안전한 리스트 교체 방식)
        if (req.getSpots() != null) {
            logger.info("Updating spots for course ID: {}", courseId);

            // 1. 기존 리스트 비우기 (orphanRemoval=true로 인해 DB 삭제)
            if (course.getCourseSpots() == null) {
                course.setCourseSpots(new ArrayList<>());
            } else {
                course.getCourseSpots().clear();
            }

            // 2. 새 스팟 추가
            if (!req.getSpots().isEmpty()) {
                List<CourseSpot> newSpots = req.getSpots().stream().map(spotDto -> CourseSpot.builder()
                        .course(course)
                        .orderNo(spotDto.getOrderNo())
                        .title(spotDto.getTitle())
                        .description(spotDto.getDescription())
                        .lat(spotDto.getLat())
                        .lng(spotDto.getLng())
                        .images(toJsonArray(spotDto.getImages()))
                        .stayMinutes(spotDto.getStayMinutes())
                        .price(spotDto.getPrice())
                        .build()).toList();

                course.getCourseSpots().addAll(newSpots);
            }
        }
        // 트랜잭션 종료 시점에 변경 감지(Dirty Checking)로 자동 저장됨

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

        // orphanRemoval=true 설정이 되어 있다면, 코스 삭제 시 스팟도 자동 삭제됨.
        // 명시적으로 지우고 싶다면 유지해도 되지만, 충돌 방지를 위해 courseRepo.delete만 호출하는 것이 깔끔함.
        // spotRepo.deleteByCourse(course); // ⬅️ 제거 (Cascade에 맡김)

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
        logger.debug("Searching courses with DTO: {} and Pageable: {}", searchDto, pageable);
        return courseRepo.searchByFilter(searchDto, pageable)
                .map(this::toSummary);
    }

    @Override
    @CacheEvict(value = {"popularCourses"}, allEntries = true)
    public Detail submitForReview(UUID courseId, String currentUserEmail) {
        Course course = courseRepo.findById(courseId)
                .orElseThrow(() -> new EntityNotFoundException("코스를 찾을 수 없습니다. ID: " + courseId));

        if (!course.getCreator().getUser().getEmail().equals(currentUserEmail)) {
            throw new AccessDeniedException("이 코스를 제출할 권한이 없습니다.");
        }
        if (course.getReviewState() != ReviewState.DRAFT && course.getReviewState() != ReviewState.REJECTED) {
            throw new IllegalStateException("이미 검토 중이거나 승인된 코스입니다.");
        }

        course.setReviewState(ReviewState.PENDING);
        return toDetail(course, currentUserEmail);
    }

    @Override
    @CacheEvict(value = {"popularCourses"}, allEntries = true)
    public Detail approve(UUID courseId) {
        Course course = courseRepo.findById(courseId)
                .orElseThrow(() -> new EntityNotFoundException("코스를 찾을 수 없습니다. ID: " + courseId));

        if (course.getReviewState() != ReviewState.PENDING) {
            throw new IllegalStateException("검토 중인 코스만 승인할 수 있습니다.");
        }

        course.setReviewState(ReviewState.APPROVED);
        course.setPublishedAt(OffsetDateTime.now());
        course.setRejectedReason(null);
        return toDetail(course, getCurrentUserEmailForAdminActions());
    }

    @Override
    @CacheEvict(value = {"popularCourses"}, allEntries = true)
    public Detail reject(UUID courseId, String reason) {
        Course course = courseRepo.findById(courseId)
                .orElseThrow(() -> new EntityNotFoundException("코스를 찾을 수 없습니다. ID: " + courseId));

        if (course.getReviewState() != ReviewState.PENDING) {
            throw new IllegalStateException("검토 중인 코스만 거절할 수 있습니다.");
        }
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("거절 사유는 필수입니다.");
        }

        course.setReviewState(ReviewState.REJECTED);
        course.setRejectedReason(reason);
        course.setPublishedAt(null);
        return toDetail(course, getCurrentUserEmailForAdminActions());
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable("popularCourses")
    public Page<CourseSummary> getPopularCourses(Pageable pageable) {
        logger.info("Fetching popular courses from DB for pageable: {}", pageable);
        return courseRepo.findByOrderByLikeCountDesc(pageable)
                .map(this::toSummary);
    }

    @Override
    @Transactional(readOnly = true)
    public RecommendationDto getCourseRecommendations(UUID courseId) {
        Course sourceCourse = courseRepo.findById(courseId)
                .orElseThrow(() -> new EntityNotFoundException("추천 기준 코스를 찾을 수 없습니다. ID: " + courseId));

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


    // --- Helper Methods ---

    private String toJsonArray(List<String> list) {
        if (list == null || list.isEmpty()) return "[]";
        try {
            return objectMapper.writeValueAsString(list);
        } catch (Exception e) {
            logger.error("Failed to serialize list to JSON array: {}", list, e);
            throw new RuntimeException("이미지 목록을 JSON으로 변환하는 데 실패했습니다.", e);
        }
    }

    private List<String> parseImages(String json) {
        if (json == null || json.isBlank() || json.equals("[]")) return List.of();
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            logger.error("Failed to parse images JSON string: {}", json, e);
            return List.of();
        }
    }

    private CourseSummary toSummary(Course c) {
        BigDecimal lat = null;
        BigDecimal lng = null;
        if (c.getCourseSpots() != null && !c.getCourseSpots().isEmpty()) {
            CourseSpot firstSpot = c.getCourseSpots().get(0);
            lat = firstSpot.getLat();
            lng = firstSpot.getLng();
        }

        return new CourseSummary(
                c.getId(), c.getTitle(), c.getSummary(), c.getCoverImageUrl(),
                c.getRegionName(), c.getDurationMinutes(), c.getEstimatedCost(),
                c.getLikeCount(), c.getPurchaseCount(), c.getReviewState(), c.getCreatedAt(), lat, lng
        );
    }

    private Detail toDetail(Course c, String currentUserEmail) {
        // spotRepo.findByCourse... 대신 Entity의 리스트 사용 (Lazy Loading 주의 - Transactional 필요)
        List<CourseSpot> spots = c.getCourseSpots();
        if(spots == null) spots = new ArrayList<>();

        List<SpotRes> spotResList = spots.stream().map(s -> new SpotRes(
                s.getOrderNo(), s.getTitle(), s.getDescription(), s.getLat(), s.getLng(),
                parseImages(s.getImages()),
                s.getStayMinutes(), s.getPrice()
        )).toList();

        boolean isLiked = false;
        if (currentUserEmail != null) {
            try {
                isLiked = likeService.isCourseLikedByUser(c.getId(), currentUserEmail);
            } catch (Exception e) {
                logger.error("Error checking like status: {}", e.getMessage());
            }
        }

        return new Detail(
                c.getId(),
                c.getCreator().getId(),
                c.getCreator().getDisplayName(),
                c.getCategory() == null ? null : c.getCategory().getSlug(),
                c.getTitle(), c.getSummary(), c.getCoverImageUrl(),
                c.getRegionCode(), c.getRegionName(), c.getDurationMinutes(), c.getEstimatedCost(),
                c.getTags() == null ? List.of() : new ArrayList<>(c.getTags()),
                c.getLikeCount(), c.getPurchaseCount(), c.getReviewState(),
                c.getPublishedAt(),
                spotResList,
                isLiked,
                c.getCreatedAt()
        );
    }

    private void processFormFields(Object reqDto) {
        if (reqDto instanceof CreateReq req) {
            if ((req.getTags() == null || req.getTags().isEmpty()) && req.getTagsString() != null && !req.getTagsString().isBlank()) {
                req.setTags(parseCommaSeparatedString(req.getTagsString()));
            }
        } else if (reqDto instanceof UpdateReq req) {
            if ((req.getTags() == null || req.getTags().isEmpty()) && req.getTagsString() != null && !req.getTagsString().isBlank()) {
                req.setTags(parseCommaSeparatedString(req.getTagsString()));
            }
        }
    }

    private String getCurrentUserEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() && !"anonymousUser".equals(authentication.getPrincipal())) {
            Object principal = authentication.getPrincipal();
            if (principal instanceof UserDetails) {
                return ((UserDetails) principal).getUsername();
            } else if (principal instanceof String) {
                return (String) principal;
            }
        }
        return null;
    }

    private String getCurrentUserEmailForAdminActions() {
        return getCurrentUserEmail();
    }

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
    public Page<CourseSummary> getMyCourses(String email, Pageable pageable) {
        User user = userRepo.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        // 크리에이터 프로필이 없으면 빈 페이지 반환
        if (user.getCreatorProfile() == null) {
            return Page.empty(pageable);
        }

        return courseRepo.findByCreator_Id(user.getCreatorProfile().getId(), pageable)
                .map(this::toSummary);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CourseSummary> getLikedCourses(String email, Pageable pageable) {
        User user = userRepo.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        return courseRepo.findLikedCoursesByUserId(user.getId(), pageable)
                .map(this::toSummary);
    }
}
*/
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
import com.capstone.Capstone_2.service.LikeService;
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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
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
    private final UserRepository userRepo;
    private final ObjectMapper objectMapper;
    private final LikeService likeService;
    private static final Logger logger = LoggerFactory.getLogger(CourseServiceImpl.class);

    @Override
    @CacheEvict(value = {"popularCourses"}, allEntries = true)
    public Detail create(CreateReq req, String creatorEmail) {
        User creatorUser = userRepo.findByEmail(creatorEmail)
                .orElseThrow(() -> new EntityNotFoundException("해당 이메일을 가진 사용자를 찾을 수 없습니다: " + creatorEmail));

        CreatorProfile creator = creatorUser.getCreatorProfile();
        if (creator == null) {
            throw new EntityNotFoundException("해당 사용자의 크리에이터 프로필이 존재하지 않습니다.");
        }

        processFormFields(req);

        Category category = null;
        if (req.getCategoryId() != null) {
            category = categoryRepo.findById(req.getCategoryId())
                    .orElseThrow(() -> new EntityNotFoundException("카테고리를 찾을 수 없습니다. ID: " + req.getCategoryId()));
        }

        // ✅ [수정] 비용 자동 계산 로직
        if (req.getEstimatedCost() == null || req.getEstimatedCost() == 0) {
            int totalSpotPrice = 0;
            if (req.getSpots() != null) {
                totalSpotPrice = req.getSpots().stream()
                        .mapToInt(spot -> spot.getPrice() != null ? spot.getPrice() : 0)
                        .sum();
            }
            req.setEstimatedCost(totalSpotPrice);
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
                .tags(req.getTags() == null ? new HashSet<>() : new HashSet<>(req.getTags()))
                .reviewState(ReviewState.DRAFT)
                .courseSpots(new ArrayList<>())
                .build();

        if (req.getSpots() != null && !req.getSpots().isEmpty()) {
            List<CourseSpot> spots = req.getSpots().stream().map(spotDto -> {
                // ✅ [수정] 이미지 문자열 파싱 로직
                List<String> imageList = spotDto.getImages();
                if ((imageList == null || imageList.isEmpty()) && spotDto.getImagesInput() != null) {
                    imageList = parseCommaSeparatedString(spotDto.getImagesInput());
                }

                return CourseSpot.builder()
                        .course(newCourse)
                        .orderNo(spotDto.getOrderNo())
                        .title(spotDto.getTitle())
                        .description(spotDto.getDescription())
                        .lat(spotDto.getLat())
                        .lng(spotDto.getLng())
                        .images(toJsonArray(imageList))
                        .stayMinutes(spotDto.getStayMinutes())
                        .price(spotDto.getPrice())
                        .build();
            }).toList();

            newCourse.getCourseSpots().addAll(spots);
        }

        courseRepo.save(newCourse);
        logger.info("New course created with ID: {}", newCourse.getId());

        return toDetail(newCourse, null);
    }

    @Override
    @CacheEvict(value = {"popularCourses"}, allEntries = true)
    public Detail update(UUID courseId, UpdateReq req, String currentUserEmail) {
        Course course = courseRepo.findById(courseId)
                .orElseThrow(() -> new EntityNotFoundException("코스를 찾을 수 없습니다. ID: " + courseId));

        if (!course.getCreator().getUser().getEmail().equals(currentUserEmail)) {
            logger.warn("Access denied for updating course ID: {}. User: {}", courseId, currentUserEmail);
            throw new AccessDeniedException("이 코스를 수정할 권한이 없습니다.");
        }

        processFormFields(req);

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
        if (req.getTags() != null) course.setTags(new HashSet<>(req.getTags()));

        if (req.getSpots() != null) {
            logger.info("Updating spots for course ID: {}", courseId);

            if (course.getCourseSpots() == null) {
                course.setCourseSpots(new ArrayList<>());
            } else {
                course.getCourseSpots().clear();
            }

            courseRepo.flush();

            if (!req.getSpots().isEmpty()) {
                List<CourseSpot> newSpots = req.getSpots().stream().map(spotDto -> {
                    // ✅ [수정] 이미지 문자열 파싱 로직 (Update 시에도 동일 적용)
                    List<String> imageList = spotDto.getImages();
                    if ((imageList == null || imageList.isEmpty()) && spotDto.getImagesInput() != null) {
                        imageList = parseCommaSeparatedString(spotDto.getImagesInput());
                    }

                    return CourseSpot.builder()
                            .course(course)
                            .orderNo(spotDto.getOrderNo())
                            .title(spotDto.getTitle())
                            .description(spotDto.getDescription())
                            .lat(spotDto.getLat())
                            .lng(spotDto.getLng())
                            .images(toJsonArray(imageList))
                            .stayMinutes(spotDto.getStayMinutes())
                            .price(spotDto.getPrice())
                            .build();
                }).toList();

                course.getCourseSpots().addAll(newSpots);
            }
        }

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
        logger.debug("Searching courses with DTO: {} and Pageable: {}", searchDto, pageable);
        return courseRepo.searchByFilter(searchDto, pageable)
                .map(this::toSummary);
    }

    @Override
    @CacheEvict(value = {"popularCourses"}, allEntries = true)
    public Detail submitForReview(UUID courseId, String currentUserEmail) {
        Course course = courseRepo.findById(courseId)
                .orElseThrow(() -> new EntityNotFoundException("코스를 찾을 수 없습니다. ID: " + courseId));

        if (!course.getCreator().getUser().getEmail().equals(currentUserEmail)) {
            throw new AccessDeniedException("이 코스를 제출할 권한이 없습니다.");
        }
        if (course.getReviewState() != ReviewState.DRAFT && course.getReviewState() != ReviewState.REJECTED) {
            throw new IllegalStateException("이미 검토 중이거나 승인된 코스입니다.");
        }

        course.setReviewState(ReviewState.PENDING);
        return toDetail(course, currentUserEmail);
    }

    @Override
    @CacheEvict(value = {"popularCourses"}, allEntries = true)
    public Detail approve(UUID courseId) {
        Course course = courseRepo.findById(courseId)
                .orElseThrow(() -> new EntityNotFoundException("코스를 찾을 수 없습니다. ID: " + courseId));

        if (course.getReviewState() != ReviewState.PENDING) {
            throw new IllegalStateException("검토 중인 코스만 승인할 수 있습니다.");
        }

        course.setReviewState(ReviewState.APPROVED);
        course.setPublishedAt(OffsetDateTime.now());
        course.setRejectedReason(null);
        return toDetail(course, getCurrentUserEmailForAdminActions());
    }

    @Override
    @CacheEvict(value = {"popularCourses"}, allEntries = true)
    public Detail reject(UUID courseId, String reason) {
        Course course = courseRepo.findById(courseId)
                .orElseThrow(() -> new EntityNotFoundException("코스를 찾을 수 없습니다. ID: " + courseId));

        if (course.getReviewState() != ReviewState.PENDING) {
            throw new IllegalStateException("검토 중인 코스만 거절할 수 있습니다.");
        }
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("거절 사유는 필수입니다.");
        }

        course.setReviewState(ReviewState.REJECTED);
        course.setRejectedReason(reason);
        course.setPublishedAt(null);
        return toDetail(course, getCurrentUserEmailForAdminActions());
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable("popularCourses")
    public Page<CourseSummary> getPopularCourses(Pageable pageable) {
        logger.info("Fetching popular courses from DB for pageable: {}", pageable);
        return courseRepo.findByOrderByLikeCountDesc(pageable)
                .map(this::toSummary);
    }

    @Override
    @Transactional(readOnly = true)
    public RecommendationDto getCourseRecommendations(UUID courseId) {
        Course sourceCourse = courseRepo.findById(courseId)
                .orElseThrow(() -> new EntityNotFoundException("추천 기준 코스를 찾을 수 없습니다. ID: " + courseId));

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


    // --- Helper Methods ---

    private String toJsonArray(List<String> list) {
        if (list == null || list.isEmpty()) return "[]";
        try {
            return objectMapper.writeValueAsString(list);
        } catch (Exception e) {
            logger.error("Failed to serialize list to JSON array: {}", list, e);
            throw new RuntimeException("이미지 목록을 JSON으로 변환하는 데 실패했습니다.", e);
        }
    }

    private List<String> parseImages(String json) {
        if (json == null || json.isBlank() || json.equals("[]")) return List.of();
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            logger.error("Failed to parse images JSON string: {}", json, e);
            return List.of();
        }
    }

    private CourseSummary toSummary(Course c) {
        BigDecimal lat = null;
        BigDecimal lng = null;
        if (c.getCourseSpots() != null && !c.getCourseSpots().isEmpty()) {
            CourseSpot firstSpot = c.getCourseSpots().get(0);
            lat = firstSpot.getLat();
            lng = firstSpot.getLng();
        }

        return new CourseSummary(
                c.getId(), c.getTitle(), c.getSummary(), c.getCoverImageUrl(),
                c.getRegionName(), c.getDurationMinutes(), c.getEstimatedCost(),
                c.getLikeCount(), c.getPurchaseCount(), c.getReviewState(), c.getCreatedAt(), lat, lng
        );
    }

    private Detail toDetail(Course c, String currentUserEmail) {
        List<CourseSpot> spots = c.getCourseSpots();
        if(spots == null) spots = new ArrayList<>();

        List<SpotRes> spotResList = spots.stream().map(s -> new SpotRes(
                s.getOrderNo(), s.getTitle(), s.getDescription(), s.getLat(), s.getLng(),
                parseImages(s.getImages()),
                s.getStayMinutes(), s.getPrice()
        )).toList();

        boolean isLiked = false;
        if (currentUserEmail != null) {
            try {
                isLiked = likeService.isCourseLikedByUser(c.getId(), currentUserEmail);
            } catch (Exception e) {
                logger.error("Error checking like status: {}", e.getMessage());
            }
        }

        return new Detail(
                c.getId(),
                c.getCreator().getId(),
                c.getCreator().getDisplayName(),
                // [수정 1] 카테고리가 없으면 null 대신 빈 문자열 반환
                c.getCategory() == null ? "" : c.getCategory().getSlug(),
                c.getTitle(),
                // [수정 2] 요약이 없으면 빈 문자열
                c.getSummary() == null ? "" : c.getSummary(),
                c.getCoverImageUrl(),
                // [수정 3] 지역 코드가 없으면 빈 문자열 (여기서 substring 에러가 많이 납니다)
                c.getRegionCode() == null ? "" : c.getRegionCode(),
                // [수정 4] 지역 이름이 없으면 빈 문자열
                c.getRegionName() == null ? "" : c.getRegionName(),
                c.getDurationMinutes(),
                c.getEstimatedCost(),
                c.getTags() == null ? List.of() : new ArrayList<>(c.getTags()),
                c.getLikeCount(),
                c.getPurchaseCount(),
                c.getReviewState(),
                c.getPublishedAt(),
                spotResList,
                isLiked,
                c.getCreatedAt()
        );
    }

    private void processFormFields(Object reqDto) {
        if (reqDto instanceof CreateReq req) {
            if ((req.getTags() == null || req.getTags().isEmpty()) && req.getTagsString() != null && !req.getTagsString().isBlank()) {
                req.setTags(parseCommaSeparatedString(req.getTagsString()));
            }
        } else if (reqDto instanceof UpdateReq req) {
            if ((req.getTags() == null || req.getTags().isEmpty()) && req.getTagsString() != null && !req.getTagsString().isBlank()) {
                req.setTags(parseCommaSeparatedString(req.getTagsString()));
            }
        }
    }

    private String getCurrentUserEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() && !"anonymousUser".equals(authentication.getPrincipal())) {
            Object principal = authentication.getPrincipal();
            if (principal instanceof UserDetails) {
                return ((UserDetails) principal).getUsername();
            } else if (principal instanceof String) {
                return (String) principal;
            }
        }
        return null;
    }

    private String getCurrentUserEmailForAdminActions() {
        return getCurrentUserEmail();
    }

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
    public Page<CourseSummary> getMyCourses(String email, Pageable pageable) {
        User user = userRepo.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));
        if (user.getCreatorProfile() == null) {
            return Page.empty(pageable);
        }
        return courseRepo.findByCreator_Id(user.getCreatorProfile().getId(), pageable)
                .map(this::toSummary);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CourseSummary> getLikedCourses(String email, Pageable pageable) {
        User user = userRepo.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));
        return courseRepo.findLikedCoursesByUserId(user.getId(), pageable)
                .map(this::toSummary);
    }
}