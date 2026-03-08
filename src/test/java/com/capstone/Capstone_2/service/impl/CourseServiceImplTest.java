package com.capstone.Capstone_2.service.impl;

import com.capstone.Capstone_2.dto.CourseDto;
import com.capstone.Capstone_2.dto.CourseSearchDto;
import com.capstone.Capstone_2.dto.RecommendationDto;
import com.capstone.Capstone_2.entity.*;
import com.capstone.Capstone_2.repository.*;
import com.capstone.Capstone_2.service.course.LikeService;
import com.capstone.Capstone_2.service.course.CourseServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils; // private 필드 값 주입용

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CourseServiceImplTest {

    @InjectMocks // 테스트 대상 클래스. @Mock 객체들이 여기에 주입됩니다.
    private CourseServiceImpl courseService;

    // --- Mock 객체들 ---
    @Mock
    private CourseRepository courseRepo;
    @Mock
    private CategoryRepository categoryRepo;
    @Mock
    private CourseSpotRepository spotRepo;
    @Mock
    private UserRepository userRepo;
    @Mock
    private LikeService likeService;
    @Mock
    private ObjectMapper objectMapper;
    @Mock
    private SecurityContext securityContext; // SecurityContextHolder 모의용
    @Mock
    private Authentication authentication; // SecurityContext 모의용
    // --- ---

    // --- 공통 테스트 데이터 ---
    private User testUser;
    private User otherUser;
    private CreatorProfile testCreator;
    private CreatorProfile otherCreator;
    private Category testCategory;
    private Course testCourse;
    private CourseSpot testSpot;
    private UUID courseId = UUID.randomUUID();
    private UUID userId = UUID.randomUUID();
    private UUID creatorId = UUID.randomUUID();
    private UUID categoryId = UUID.randomUUID();
    private String userEmail = "testuser@example.com";
    private String otherUserEmail = "other@example.com";
    // --- ---

    @BeforeEach
    void setUp() {
        // 공통 테스트 데이터 설정
        testUser = User.builder()
                .id(userId)
                .email(userEmail)
                .nickname("testuser")
                .role(UserRole.USER)
                .build();

        testCreator = CreatorProfile.builder()
                .id(creatorId)
                .user(testUser)
                .displayName("Test Creator")
                .build();

        testUser.setCreatorProfile(testCreator); // 양방향 연관관계 설정

        otherUser = User.builder()
                .id(UUID.randomUUID())
                .email(otherUserEmail)
                .nickname("otheruser")
                .role(UserRole.USER)
                .build();

        otherCreator = CreatorProfile.builder()
                .id(UUID.randomUUID())
                .user(otherUser)
                .displayName("Other Creator")
                .build();

        otherUser.setCreatorProfile(otherCreator);

        testCategory = Category.builder()
                .id(categoryId)
                .name("테스트 카테고리")
                .slug("test-category")
                .build();

        testSpot = CourseSpot.builder()
                .id(UUID.randomUUID())
                .orderNo(1)
                .title("테스트 스팟")
                .images("[]")
                .build();

        testCourse = Course.builder()
                .id(courseId)
                .creator(testCreator)
                .category(testCategory)
                .title("Test Course")
                .summary("Test Summary")
                .reviewState(ReviewState.DRAFT) // 기본 상태 DRAFT
                .likeCount(0)
                .purchaseCount(0)
                .tags(new HashSet<>(Set.of("tag1", "tag2")))
                .regionCode("11000")
                .build();

        // BaseTimeEntity의 createdAt 필드 모의 설정 (DTO 변환 테스트용)
        ReflectionTestUtils.setField(testCourse, "createdAt", LocalDateTime.now().minusDays(1));
    }

    @AfterEach
    void tearDown() {
        // 각 테스트 후 SecurityContext 클리어
        SecurityContextHolder.clearContext();
    }

    // --- SecurityContext 모의 헬퍼 ---
    private void mockSecurityContext(String email) {
        UserDetails userDetails = mock(UserDetails.class);
        given(userDetails.getUsername()).willReturn(email);
        given(authentication.getPrincipal()).willReturn(userDetails);
        given(authentication.isAuthenticated()).willReturn(true);
        given(securityContext.getAuthentication()).willReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
    }

    // --- create 테스트 ---

    @Test
    @DisplayName("코스 생성 테스트 - 성공")
    void createCourse_success() throws Exception {
        // Given
        CourseDto.CreateReq req = new CourseDto.CreateReq();
        req.setTitle("New Course");
        req.setSummary("New Summary");
        req.setCategoryId(categoryId);
        req.setTagsString("tag1, tag2"); // processFormFields 테스트용

        given(userRepo.findByEmail(userEmail)).willReturn(Optional.of(testUser));
        given(categoryRepo.findById(categoryId)).willReturn(Optional.of(testCategory));
        given(courseRepo.save(any(Course.class))).willAnswer(invocation -> invocation.getArgument(0)); // 저장 객체 반환

        // toDetail 내부의 likeService 모의 (생성 시엔 null 이메일 전달)
        given(spotRepo.findByCourseOrderByOrderNoAsc(any(Course.class))).willReturn(List.of());

        // When
        CourseDto.Detail result = courseService.create(req, userEmail);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.title()).isEqualTo("New Course");
        assertThat(result.summary()).isEqualTo("New Summary");
        assertThat(result.categorySlug()).isEqualTo("test-category");
        assertThat(result.creatorDisplayName()).isEqualTo("Test Creator");
        assertThat(result.tags()).contains("tag1", "tag2");
        verify(courseRepo, times(1)).save(any(Course.class));
    }

    @Test
    @DisplayName("코스 생성 테스트 - 실패 (사용자 없음)")
    void createCourse_fail_userNotFound() {
        // Given
        CourseDto.CreateReq req = new CourseDto.CreateReq();
        given(userRepo.findByEmail(userEmail)).willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> courseService.create(req, userEmail))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("사용자를 찾을 수 없습니다");
    }

    @Test
    @DisplayName("코스 생성 테스트 - 실패 (크리에이터 프로필 없음)")
    void createCourse_fail_creatorProfileNotFound() {
        // Given
        CourseDto.CreateReq req = new CourseDto.CreateReq();
        testUser.setCreatorProfile(null); // 프로필 제거
        given(userRepo.findByEmail(userEmail)).willReturn(Optional.of(testUser));

        // When & Then
        assertThatThrownBy(() -> courseService.create(req, userEmail))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("크리에이터 프로필이 존재하지 않습니다");
    }

    @Test
    @DisplayName("코스 생성 테스트 - 실패 (카테고리 없음)")
    void createCourse_fail_categoryNotFound() {
        // Given
        CourseDto.CreateReq req = new CourseDto.CreateReq();
        req.setCategoryId(categoryId);
        given(userRepo.findByEmail(userEmail)).willReturn(Optional.of(testUser));
        given(categoryRepo.findById(categoryId)).willReturn(Optional.empty()); // 카테고리 없음

        // When & Then
        assertThatThrownBy(() -> courseService.create(req, userEmail))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("카테고리를 찾을 수 없습니다");
    }

    // --- get (상세 조회) 테스트 ---

    @Test
    @DisplayName("코스 상세 조회 (get) 테스트 - 성공 (로그인 사용자, 좋아요 함)")
    void getCourseDetail_success_loggedIn_liked() {
        // Given
        // ✅ 2. Repository/Service 모의
        given(courseRepo.findById(courseId)).willReturn(Optional.of(testCourse));
        given(likeService.isCourseLikedByUser(courseId, userEmail)).willReturn(true); // 좋아요 누른 상태
        given(spotRepo.findByCourseOrderByOrderNoAsc(testCourse)).willReturn(List.of(testSpot));

        // When
        // ✅ 3. public 'get' 메소드 호출
        CourseDto.Detail result = courseService.get(courseId, userEmail); // ⬅️ 수정됨

        // Then
        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(courseId);
        assertThat(result.title()).isEqualTo("Test Course");
        assertThat(result.isCurrentUserLiked()).isTrue(); // 좋아요 상태 검증
        assertThat(result.spots()).hasSize(1);
        assertThat(result.spots().get(0).title()).isEqualTo("테스트 스팟");
        assertThat(result.createdAt()).isNotNull(); // 생성 시간 검증
    }

    @Test
    @DisplayName("코스 상세 조회 (get) 테스트 - 성공 (비로그인 사용자)")
    void getCourseDetail_success_anonymous() {
        // Given
        // SecurityContextHolder를 모의하지 않음 (기본값: 비로그인)
        given(courseRepo.findById(courseId)).willReturn(Optional.of(testCourse));
        given(spotRepo.findByCourseOrderByOrderNoAsc(testCourse)).willReturn(List.of());

        // When
        CourseDto.Detail result = courseService.get(courseId, null); // ⬅️ 수정됨

        // Then
        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(courseId);
        assertThat(result.isCurrentUserLiked()).isFalse(); // 비로그인 시 항상 false
        // likeService.isCourseLikedByUser가 호출되지 않았는지 검증
        verify(likeService, never()).isCourseLikedByUser(any(), any());
    }

    @Test
    @DisplayName("코스 상세 조회 (get) 테스트 - 실패 (코스 없음)")
    void getCourseDetail_fail_notFound() {
        // Given
        given(courseRepo.findById(courseId)).willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> courseService.get(courseId, null))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("코스를 찾을 수 없습니다");
    }

    // --- update 테스트 ---

    @Test
    @DisplayName("코스 수정 테스트 - 성공 (소유자)")
    void updateCourse_success_byOwner() {
        // Given
        CourseDto.UpdateReq req = new CourseDto.UpdateReq();
        req.setTitle("Updated Title");
        req.setSpots(List.of()); // 스팟 목록을 비움

        given(courseRepo.findById(courseId)).willReturn(Optional.of(testCourse));
        // toDetail 모의
        given(likeService.isCourseLikedByUser(courseId, userEmail)).willReturn(false);
        given(spotRepo.findByCourseOrderByOrderNoAsc(any(Course.class))).willReturn(List.of());

        // When
        CourseDto.Detail result = courseService.update(courseId, req, userEmail);

        // Then
        assertThat(result.title()).isEqualTo("Updated Title");
        assertThat(testCourse.getTitle()).isEqualTo("Updated Title"); // 원본 객체가 변경되었는지 확인
        verify(spotRepo, times(1)).deleteByCourse(testCourse); // 스팟 삭제 호출 확인
    }

    @Test
    @DisplayName("코스 수정 테스트 - 실패 (소유자 아님)")
    void updateCourse_fail_notOwner() {
        // Given
        CourseDto.UpdateReq req = new CourseDto.UpdateReq();
        given(courseRepo.findById(courseId)).willReturn(Optional.of(testCourse));
        // testCourse의 소유자는 "testuser@example.com"

        // When & Then
        assertThatThrownBy(() -> courseService.update(courseId, req, otherUserEmail))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("이 코스를 수정할 권한이 없습니다.");
    }

    @Test
    @DisplayName("코스 수정 테스트 - 실패 (코스 없음)")
    void updateCourse_fail_notFound() {
        // Given
        CourseDto.UpdateReq req = new CourseDto.UpdateReq();
        given(courseRepo.findById(courseId)).willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> courseService.update(courseId, req, userEmail))
                .isInstanceOf(EntityNotFoundException.class);
    }

    // --- delete 테스트 ---

    @Test
    @DisplayName("코스 삭제 테스트 - 성공 (소유자)")
    void deleteCourse_success_byOwner() {
        // Given
        given(courseRepo.findById(courseId)).willReturn(Optional.of(testCourse));
        doNothing().when(spotRepo).deleteByCourse(testCourse);
        doNothing().when(courseRepo).delete(testCourse);

        // When
        courseService.delete(courseId, userEmail);

        // Then
        verify(spotRepo, times(1)).deleteByCourse(testCourse);
        verify(courseRepo, times(1)).delete(testCourse);
    }

    @Test
    @DisplayName("코스 삭제 테스트 - 실패 (소유자 아님)")
    void deleteCourse_fail_notOwner() {
        // Given
        given(courseRepo.findById(courseId)).willReturn(Optional.of(testCourse));

        // When & Then
        assertThatThrownBy(() -> courseService.delete(courseId, otherUserEmail))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("이 코스를 삭제할 권한이 없습니다.");
    }

    // --- search 테스트 ---

    @Test
    @DisplayName("코스 검색(search) 테스트")
    void searchCourses() {
        // Given
        CourseSearchDto searchDto = new CourseSearchDto();
        searchDto.setQ("Test");
        Pageable pageable = PageRequest.of(0, 10);

        Page<Course> mockedPage = new PageImpl<>(List.of(testCourse), pageable, 1);
        given(courseRepo.searchByFilter(searchDto, pageable)).willReturn(mockedPage);

        // When
        Page<CourseDto.CourseSummary> result = courseService.search(searchDto, pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).title()).isEqualTo("Test Course");
        verify(courseRepo, times(1)).searchByFilter(searchDto, pageable);
    }

    // --- 상태 변경(submit, approve, reject) 테스트 ---

    @Test
    @DisplayName("리뷰 제출 (submitForReview) - 실패 (잘못된 상태)")
    void submitForReview_fail_wrongState() {
        // Given
        testCourse.setReviewState(ReviewState.APPROVED); // 이미 승인된 상태
        given(courseRepo.findById(courseId)).willReturn(Optional.of(testCourse));

        // When & Then
        assertThatThrownBy(() -> courseService.submitForReview(courseId, userEmail))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("이미 검토 중이거나 승인된 코스입니다.");
    }

    @Test
    @DisplayName("코스 승인 (approve) - 성공")
    void approve_success() {
        // Given
        mockSecurityContext("admin@example.com"); // 관리자 계정 모의 (toDetail용)
        testCourse.setReviewState(ReviewState.PENDING); // PENDING 상태
        given(courseRepo.findById(courseId)).willReturn(Optional.of(testCourse));
        given(likeService.isCourseLikedByUser(courseId, "admin@example.com")).willReturn(false);
        given(spotRepo.findByCourseOrderByOrderNoAsc(testCourse)).willReturn(List.of());

        // When
        CourseDto.Detail result = courseService.approve(courseId);

        // Then
        assertThat(result.reviewState()).isEqualTo(ReviewState.APPROVED);
        assertThat(result.publishedAt()).isNotNull().isBeforeOrEqualTo(OffsetDateTime.now());
        assertThat(testCourse.getReviewState()).isEqualTo(ReviewState.APPROVED);
        assertThat(testCourse.getPublishedAt()).isNotNull();
    }

    @Test
    @DisplayName("코스 승인 (approve) - 실패 (잘못된 상태)")
    void approve_fail_wrongState() {
        // Given
        testCourse.setReviewState(ReviewState.DRAFT); // PENDING 상태가 아님
        given(courseRepo.findById(courseId)).willReturn(Optional.of(testCourse));

        // When & Then
        assertThatThrownBy(() -> courseService.approve(courseId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("검토 중인 코스만 승인할 수 있습니다.");
    }

    @Test
    @DisplayName("코스 거절 (reject) - 성공")
    void reject_success() {
        // Given
        mockSecurityContext("admin@example.com");
        testCourse.setReviewState(ReviewState.PENDING);
        given(courseRepo.findById(courseId)).willReturn(Optional.of(testCourse));
        given(likeService.isCourseLikedByUser(courseId, "admin@example.com")).willReturn(false);
        given(spotRepo.findByCourseOrderByOrderNoAsc(testCourse)).willReturn(List.of());

        String reason = "부적절한 내용";

        // When
        CourseDto.Detail result = courseService.reject(courseId, reason);

        // Then
        assertThat(result.reviewState()).isEqualTo(ReviewState.REJECTED);
        assertThat(testCourse.getReviewState()).isEqualTo(ReviewState.REJECTED);
        assertThat(testCourse.getRejectedReason()).isEqualTo(reason);
    }

    @Test
    @DisplayName("코스 거절 (reject) - 실패 (사유 없음)")
    void reject_fail_noReason() {
        // Given
        testCourse.setReviewState(ReviewState.PENDING);
        given(courseRepo.findById(courseId)).willReturn(Optional.of(testCourse));

        // When & Then
        assertThatThrownBy(() -> courseService.reject(courseId, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> courseService.reject(courseId, "  "))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // --- 추천 로직 테스트 ---

    @Test
    @DisplayName("인기 코스 조회 (getPopularCourses) - 성공")
    void getPopularCourses_success() {
        // Given
        Pageable pageable = PageRequest.of(0, 5);
        Course popularCourse = Course.builder().title("Popular").creator(testCreator).build();
        Page<Course> mockedPage = new PageImpl<>(List.of(popularCourse), pageable, 1);

        given(courseRepo.findByOrderByLikeCountDesc(pageable)).willReturn(mockedPage);

        // When
        Page<CourseDto.CourseSummary> result = courseService.getPopularCourses(pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).title()).isEqualTo("Popular");
        verify(courseRepo, times(1)).findByOrderByLikeCountDesc(pageable);
    }

    @Test
    @DisplayName("코스 추천 (getCourseRecommendations) - 성공 (모든 조건)")
    void getCourseRecommendations_success_all() {
        // Given
        Pageable limit = PageRequest.of(0, 5);
        Course relatedByLike = Course.builder().title("Liked").creator(testCreator).build();
        Course sameCategory = Course.builder().title("Category").creator(testCreator).build();
        Course sameRegion = Course.builder().title("Region").creator(testCreator).build();

        given(courseRepo.findById(courseId)).willReturn(Optional.of(testCourse));
        given(courseRepo.findRelatedCoursesByLikes(courseId, limit)).willReturn(List.of(relatedByLike));
        given(courseRepo.findByCategoryAndIdNotOrderByLikeCountDesc(testCategory, courseId, limit))
                .willReturn(new PageImpl<>(List.of(sameCategory)));
        given(courseRepo.findByRegionCodeAndIdNotOrderByLikeCountDesc("11000", courseId, limit))
                .willReturn(new PageImpl<>(List.of(sameRegion)));

        // When
        RecommendationDto result = courseService.getCourseRecommendations(courseId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getRelatedByLikes()).hasSize(1).extracting("title").contains("Liked");
        assertThat(result.getSameCategory()).hasSize(1).extracting("title").contains("Category");
        assertThat(result.getSameRegion()).hasSize(1).extracting("title").contains("Region");
    }

    @Test
    @DisplayName("코스 추천 (getCourseRecommendations) - 카테고리/지역 없는 경우")
    void getCourseRecommendations_success_partial() {
        // Given
        testCourse.setCategory(null); // 카테고리 없음
        testCourse.setRegionCode(null); // 지역 없음
        Pageable limit = PageRequest.of(0, 5);

        given(courseRepo.findById(courseId)).willReturn(Optional.of(testCourse));
        given(courseRepo.findRelatedCoursesByLikes(courseId, limit)).willReturn(List.of());
        // findByCategory... 와 findByRegion...은 호출되지 않아야 함

        // When
        RecommendationDto result = courseService.getCourseRecommendations(courseId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getRelatedByLikes()).isEmpty();
        assertThat(result.getSameCategory()).isEmpty(); // 빈 리스트
        assertThat(result.getSameRegion()).isEmpty(); // 빈 리스트
        // 해당 메소드들이 호출되지 않았는지 검증
        verify(courseRepo, never()).findByCategoryAndIdNotOrderByLikeCountDesc(any(), any(), any());
        verify(courseRepo, never()).findByRegionCodeAndIdNotOrderByLikeCountDesc(any(), any(), any());
    }
}