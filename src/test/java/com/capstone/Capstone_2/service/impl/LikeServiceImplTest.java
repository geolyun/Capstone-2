package com.capstone.Capstone_2.service.impl;

import com.capstone.Capstone_2.dto.LikeDto;
import com.capstone.Capstone_2.entity.Course;
import com.capstone.Capstone_2.entity.Like;
import com.capstone.Capstone_2.entity.LikeId;
import com.capstone.Capstone_2.entity.User;
import com.capstone.Capstone_2.repository.CourseRepository;
import com.capstone.Capstone_2.repository.LikeRepository;
import com.capstone.Capstone_2.repository.UserRepository;
import com.capstone.Capstone_2.service.course.LikeServiceImpl;

import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class LikeServiceImplTest {

    @InjectMocks
    private LikeServiceImpl likeService;

    @Mock
    private LikeRepository likeRepo;
    @Mock
    private UserRepository userRepo;
    @Mock
    private CourseRepository courseRepo;

    private User testUser;
    private Course testCourse;
    private LikeId testLikeId;
    private String userEmail = "testuser@example.com";
    private UUID userId = UUID.randomUUID();
    private UUID courseId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        testUser = User.builder().id(userId).email(userEmail).build();
        testCourse = Course.builder().id(courseId).likeCount(0).build();
        testLikeId = new LikeId(userId, courseId);
    }

    @Test
    @DisplayName("좋아요 토글 - '좋아요' 추가 (Like 생성)")
    void toggleLike_shouldAddLike_whenNotExists() {
        // Given
        given(userRepo.findByEmail(userEmail)).willReturn(Optional.of(testUser));
        given(courseRepo.findById(courseId)).willReturn(Optional.of(testCourse));
        given(likeRepo.existsById(testLikeId)).willReturn(false); // 좋아요가 존재하지 않음

        // When
        LikeDto result = likeService.toggleLike(courseId, userEmail);

        // Then
        // 1. DTO 검증
        assertThat(result.liked()).isTrue();
        assertThat(result.likeCount()).isEqualTo(1);
        assertThat(result.userId()).isEqualTo(userId);

        // 2. Course 엔티티의 likeCount가 1 증가했는지 검증
        assertThat(testCourse.getLikeCount()).isEqualTo(1);

        // 3. likeRepo.save가 호출되었는지 검증
        ArgumentCaptor<Like> likeCaptor = ArgumentCaptor.forClass(Like.class);
        verify(likeRepo).save(likeCaptor.capture());
        assertThat(likeCaptor.getValue().getId()).isEqualTo(testLikeId);
    }

    @Test
    @DisplayName("좋아요 토글 - '좋아요' 취소 (Like 삭제)")
    void toggleLike_shouldRemoveLike_whenExists() {
        // Given
        testCourse.setLikeCount(5); // 기존 좋아요 수
        given(userRepo.findByEmail(userEmail)).willReturn(Optional.of(testUser));
        given(courseRepo.findById(courseId)).willReturn(Optional.of(testCourse));
        given(likeRepo.existsById(testLikeId)).willReturn(true); // 좋아요가 이미 존재함

        // When
        LikeDto result = likeService.toggleLike(courseId, userEmail);

        // Then
        // 1. DTO 검증
        assertThat(result.liked()).isFalse();
        assertThat(result.likeCount()).isEqualTo(4); // 5 -> 4
        assertThat(result.userId()).isEqualTo(userId);

        // 2. Course 엔티티의 likeCount가 1 감소했는지 검증
        assertThat(testCourse.getLikeCount()).isEqualTo(4);

        // 3. likeRepo.deleteById가 호출되었는지 검증
        verify(likeRepo).deleteById(testLikeId);
        verify(likeRepo, never()).save(any()); // save는 호출되지 않아야 함
    }

    @Test
    @DisplayName("좋아요 토글 - 실패 (사용자 없음)")
    void toggleLike_fail_userNotFound() {
        // Given
        given(userRepo.findByEmail(userEmail)).willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> likeService.toggleLike(courseId, userEmail))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("User not found");
    }

    @Test
    @DisplayName("좋아요 토글 - 실패 (코스 없음)")
    void toggleLike_fail_courseNotFound() {
        // Given
        given(userRepo.findByEmail(userEmail)).willReturn(Optional.of(testUser));
        given(courseRepo.findById(courseId)).willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> likeService.toggleLike(courseId, userEmail))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Course not found");
    }

    @Test
    @DisplayName("좋아요 상태 확인 - True (좋아요 누름)")
    void isCourseLikedByUser_shouldReturnTrue_whenLikeExists() {
        // Given
        given(userRepo.findByEmail(userEmail)).willReturn(Optional.of(testUser));
        given(courseRepo.existsById(courseId)).willReturn(true);
        given(likeRepo.existsById(testLikeId)).willReturn(true);

        // When
        boolean result = likeService.isCourseLikedByUser(courseId, userEmail);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("좋아요 상태 확인 - False (좋아요 안 누름)")
    void isCourseLikedByUser_shouldReturnFalse_whenLikeNotExists() {
        // Given
        given(userRepo.findByEmail(userEmail)).willReturn(Optional.of(testUser));
        given(courseRepo.existsById(courseId)).willReturn(true);
        given(likeRepo.existsById(testLikeId)).willReturn(false);

        // When
        boolean result = likeService.isCourseLikedByUser(courseId, userEmail);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("좋아요 상태 확인 - False (사용자 없음)")
    void isCourseLikedByUser_shouldReturnFalse_whenUserNotFound() {
        // Given
        given(userRepo.findByEmail(userEmail)).willReturn(Optional.empty());

        // When
        boolean result = likeService.isCourseLikedByUser(courseId, userEmail);

        // Then
        assertThat(result).isFalse();
        verify(likeRepo, never()).existsById(any()); // 사용자가 없으므로 LikeRepo 조회 안 함
    }

    @Test
    @DisplayName("좋아요 상태 확인 - False (코스 없음)")
    void isCourseLikedByUser_shouldReturnFalse_whenCourseNotFound() {
        // Given
        given(userRepo.findByEmail(userEmail)).willReturn(Optional.of(testUser));
        given(courseRepo.existsById(courseId)).willReturn(false);

        // When
        boolean result = likeService.isCourseLikedByUser(courseId, userEmail);

        // Then
        assertThat(result).isFalse();
        verify(likeRepo, never()).existsById(any()); // 코스가 없으므로 LikeRepo 조회 안 함
    }
}