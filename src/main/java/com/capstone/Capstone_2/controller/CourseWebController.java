package com.capstone.Capstone_2.controller;

import com.capstone.Capstone_2.dto.CourseDto;
import com.capstone.Capstone_2.dto.CourseSearchDto;
import com.capstone.Capstone_2.dto.RecommendationDto;
import com.capstone.Capstone_2.dto.UserPrincipal;
import com.capstone.Capstone_2.service.CourseService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.AccessDeniedException;

import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/courses")
@RequiredArgsConstructor
public class CourseWebController {

    private final CourseService courseService;
    private final ObjectMapper objectMapper;
    private static final Logger logger = LoggerFactory.getLogger(CourseWebController.class);


    @GetMapping
    public String courseListPage(
            // @RequestParam String keyword -> @ModelAttribute CourseSearchDto searchDto
            @ModelAttribute("searchDto") CourseSearchDto searchDto,
            @PageableDefault(size = 9, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            Model model) {

        // 서비스 호출 시 searchDto 전달
        Page<CourseDto.CourseSummary> coursePage = courseService.search(searchDto, pageable);

        model.addAttribute("coursePage", coursePage);
        // 모델에 searchDto 추가 (폼 값 유지 및 페이지네이션 링크용)
        // model.addAttribute("keyword", keyword); // 삭제 또는 searchDto.q 사용
        // model.addAttribute("searchDto", searchDto); // @ModelAttribute 사용 시 자동으로 추가됨

        return "courses/list";
    }

    @GetMapping("/new")
    public String courseForm(Model model) {
        model.addAttribute("course", new CourseDto.CreateReq());
        return "courses/course-form";
    }

    // --- 새 코스 만들기 처리 (폼 제출) ---
    @PostMapping // "/courses" 경로의 POST 요청 처리
    public String createCourse(
            @Valid @ModelAttribute("course") CourseDto.CreateReq createReq,
            BindingResult bindingResult, // ✅ 유효성 검사 결과
            @AuthenticationPrincipal UserPrincipal principal,
            Model model) { // ✅ 에러 전달을 위한 Model

        logger.info("Received course creation request via web form: {}", createReq.getTitle());

        // ✅ 1. 기본 유효성 검사 (NotBlank, NotNull 등)
        if (bindingResult.hasErrors()) {
            logger.warn("Validation errors on course form: {}", bindingResult.getAllErrors());
            return "courses/course-form"; // 에러 시 다시 폼 페이지로
        }

        try {
            // ✅ 2. tagsString 파싱
            if (createReq.getTagsString() != null && !createReq.getTagsString().isBlank()) {
                List<String> tags = Arrays.stream(createReq.getTagsString().split(","))
                        .map(String::trim).filter(tag -> !tag.isEmpty()).toList();
                createReq.setTags(tags);
            }

            // ✅ 3. metadataJson 파싱
            if (createReq.getMetadataJson() != null && !createReq.getMetadataJson().isBlank()) {
                Map<String, Object> metadata = objectMapper.readValue(
                        createReq.getMetadataJson(), new TypeReference<>() {});
                createReq.setMetadata(metadata);
            }

            // 서비스 호출
            courseService.create(createReq, principal.getUsername());
            logger.info("Course created successfully via web form!");
            return "redirect:/home"; // 성공 시 홈으로

        } catch (Exception e) {
            logger.error("Error creating course via web form: {}", e.getMessage(), e);
            model.addAttribute("errorMessage", "코스 생성 중 오류 발생: " + e.getMessage());
            // 에러 발생 시 입력값 유지를 위해 createReq를 다시 모델에 넣어줌
            model.addAttribute("course", createReq);
            return "courses/course-form"; // 에러 시 다시 폼 페이지로
        }
    }

    @GetMapping("/{courseId}/recommendations-page")
    public String showRecommendationsPage(@PathVariable UUID courseId, Model model) {
        RecommendationDto recommendations = courseService.getCourseRecommendations(courseId);
        CourseDto.Detail sourceCourse = courseService.get(courseId);

        model.addAttribute("sourceCourse", sourceCourse);
        model.addAttribute("recommendations", recommendations);

        return "courses/recommendations";
    }

    @GetMapping("/{courseId}")
    public String courseDetailPage(@PathVariable UUID courseId, Model model) {
        try {
            CourseDto.Detail courseDetail = courseService.get(courseId);
            model.addAttribute("course", courseDetail);
            return "courses/detail"; // templates/courses/detail.html 뷰를 렌더링
        } catch (Exception e) {
            // 코스를 찾을 수 없는 경우 등 예외 처리
            // e.printStackTrace(); // 실제로는 로깅
            return "error/404"; // 예: 404 에러 페이지로 이동
        }
    }

    @GetMapping("/{courseId}/edit")
    public String courseEditForm(@PathVariable UUID courseId, Model model, @AuthenticationPrincipal UserPrincipal principal) {
        try {
            CourseDto.Detail courseDetail = courseService.get(courseId);

            // 본인 확인 (서비스 레이어에서 이미 처리하지만, 컨트롤러에서도 확인 가능)
            if (!courseDetail.creatorId().equals(principal.getUser().getCreatorProfile().getId())) {
                // 혹은 principal.getUsername().equals(courseDetail.getCreatorEmail()) 등으로 확인
                throw new AccessDeniedException("수정 권한이 없습니다.");
            }

            // Detail DTO -> UpdateReq DTO 변환 (폼 바인딩을 위해)
            CourseDto.UpdateReq updateReq = mapDetailToUpdateReq(courseDetail); // 이 메소드는 아래에 추가합니다.

            model.addAttribute("course", updateReq);
            model.addAttribute("courseId", courseId); // 폼 action URL 생성을 위해 ID 전달
            return "courses/course-edit-form"; // 수정 폼 템플릿 경로
        } catch (EntityNotFoundException e) {
            // 존재하지 않는 코스 처리
            return "error/404";
        } catch (AccessDeniedException e) {
            // 권한 없음 처리 (예: 에러 페이지 또는 리다이렉트)
            // model.addAttribute("errorMessage", e.getMessage());
            return "redirect:/courses/" + courseId + "?error=permission_denied";
        }
    }

    // --- 코스 수정 폼 제출 처리 ---
    @PostMapping("/{courseId}/edit")
    public String updateCourse(
            @PathVariable UUID courseId,
            @Valid @ModelAttribute("course") CourseDto.UpdateReq updateReq,
            BindingResult bindingResult,
            @AuthenticationPrincipal UserPrincipal principal,
            Model model) {

        logger.info("Received course update request via web form for course ID: {}", courseId);

        if (bindingResult.hasErrors()) {
            logger.warn("Validation errors on course edit form: {}", bindingResult.getAllErrors());
            model.addAttribute("courseId", courseId); // 에러 시 ID 다시 전달
            return "courses/course-edit-form";
        }

        try {
            // 서비스 호출 시 현재 사용자 이메일 전달 (권한 확인용)
            courseService.update(courseId, updateReq, principal.getUsername());
            logger.info("Course updated successfully via web form!");
            return "redirect:/courses/" + courseId; // 성공 시 상세 페이지로 리다이렉트

        } catch (EntityNotFoundException e) {
            // 코스 또는 카테고리를 찾을 수 없는 경우
            logger.error("Error updating course: {}", e.getMessage(), e);
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("courseId", courseId);
            return "courses/course-edit-form";
        } catch (AccessDeniedException e) {
            // 권한 오류
            logger.warn("Access denied during course update: {}", e.getMessage());
            // 권한 없음 에러 처리 (예: 상세 페이지로 리다이렉트하며 에러 메시지 전달)
            return "redirect:/courses/" + courseId + "?error=permission_denied";
        } catch (Exception e) {
            // 기타 예외 처리 (JSON 파싱 오류 등)
            logger.error("Unexpected error updating course: {}", e.getMessage(), e);
            model.addAttribute("errorMessage", "코스 수정 중 오류 발생: " + e.getMessage());
            model.addAttribute("courseId", courseId);
            model.addAttribute("course", updateReq); // 입력값 유지
            return "courses/course-edit-form";
        }
    }

    // --- Helper Method: Detail DTO -> UpdateReq DTO 변환 ---
    private CourseDto.UpdateReq mapDetailToUpdateReq(CourseDto.Detail detail) {
        CourseDto.UpdateReq updateReq = new CourseDto.UpdateReq();
        // categoryId는 slug로부터 다시 찾아야 하므로 여기서는 null 또는 필요 시 서비스/리포지토리 조회
        // updateReq.setCategoryId(findCategoryIdBySlug(detail.categorySlug()));
        updateReq.setTitle(detail.title());
        updateReq.setSummary(detail.summary());
        updateReq.setCoverImageUrl(detail.coverImageUrl());
        updateReq.setRegionCode(detail.regionCode());
        updateReq.setRegionName(detail.regionName());
        updateReq.setDurationMinutes(detail.durationMinutes());
        updateReq.setEstimatedCost(detail.estimatedCost());
        updateReq.setTags(new ArrayList<>(detail.tags())); // Set -> List
        updateReq.setTagsString(String.join(", ", detail.tags())); // 웹 폼용 문자열
        updateReq.setMetadata(detail.metadata());
        try {
            updateReq.setMetadataJson(objectMapper.writeValueAsString(detail.metadata())); // 웹 폼용 JSON 문자열
        } catch (Exception e) { updateReq.setMetadataJson(""); } // JSON 변환 실패 시 빈 문자열

        // SpotRes -> SpotReq 변환
        if (detail.spots() != null) {
            List<CourseDto.SpotReq> spotReqs = detail.spots().stream().map(spotRes -> {
                CourseDto.SpotReq spotReq = new CourseDto.SpotReq();
                spotReq.setOrderNo(spotRes.orderNo());
                spotReq.setTitle(spotRes.title());
                spotReq.setDescription(spotRes.description());
                spotReq.setLat(spotRes.lat());
                spotReq.setLng(spotRes.lng());
                spotReq.setImages(new ArrayList<>(spotRes.images())); // 이미지 List 복사
                spotReq.setStayMinutes(spotRes.stayMinutes());
                spotReq.setPrice(spotRes.price());
                return spotReq;
            }).collect(Collectors.toList());
            updateReq.setSpots(spotReqs);
        }

        return updateReq;
    }

    @PostMapping("/{courseId}/delete")
    // RedirectAttributes 파라미터 추가
    public String deleteCourse(@PathVariable UUID courseId, @AuthenticationPrincipal UserPrincipal principal, RedirectAttributes redirectAttributes) {
        logger.info("Received course delete request via web form for course ID: {}", courseId);
        try {
            courseService.delete(courseId, principal.getUsername());
            logger.info("Course deleted successfully: {}", courseId);
            // 삭제 성공 메시지 전달 (선택 사항)
            redirectAttributes.addFlashAttribute("successMessage", "코스가 성공적으로 삭제되었습니다."); // 이제 정상 작동
            return "redirect:/courses"; // 성공 시 코스 목록으로 리다이렉트
        } catch (EntityNotFoundException e) {
            logger.warn("Attempted to delete non-existent course: {}", courseId);
            redirectAttributes.addFlashAttribute("errorMessage", "삭제하려는 코스를 찾을 수 없습니다."); // 이제 정상 작동
            return "redirect:/courses";
        } catch (AccessDeniedException e) {
            logger.warn("Access denied during course deletion attempt for course ID: {}", courseId);
            redirectAttributes.addFlashAttribute("errorMessage", "코스를 삭제할 권한이 없습니다."); // 이제 정상 작동
            return "redirect:/courses/" + courseId; // 실패 시 상세 페이지로 (또는 목록)
        } catch (Exception e) {
            logger.error("Error deleting course {}: {}", courseId, e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", "코스 삭제 중 오류가 발생했습니다."); // 이제 정상 작동
            return "redirect:/courses/" + courseId; // 실패 시 상세 페이지로
        }
    }
}

/*
package com.capstone.Capstone_2.controller;

import com.capstone.Capstone_2.dto.CourseDto;
import com.capstone.Capstone_2.dto.UserPrincipal;
import com.capstone.Capstone_2.service.CourseService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@Controller
@RequestMapping("/courses")
@RequiredArgsConstructor
public class CourseFormController {

    private final CourseService courseService;
    private final ObjectMapper objectMapper;

    @GetMapping("/new")
    public String courseForm(Model model) {
        model.addAttribute("course", new CourseDto.CreateReq());
        return "courses/course-form";
    }

    // ✅ "course-form.html"에서 폼 제출(POST) 시 코스를 생성
    @PostMapping
    public String createCourse(@ModelAttribute("course") CourseDto.CreateReq createReq,
                               @AuthenticationPrincipal UserPrincipal principal) {

        // ✅ 1. tagsString 파싱 (쉼표 구분 -> List<String>)
        if (createReq.getTagsString() != null && !createReq.getTagsString().isBlank()) {
            List<String> tags = Arrays.stream(createReq.getTagsString().split(","))
                    .map(String::trim)
                    .filter(tag -> !tag.isEmpty())
                    .collect(Collectors.toList());
            createReq.setTags(tags);
        }

        // ✅ 2. metadataJson 파싱 (JSON String -> Map<String, Object>)
        if (createReq.getMetadataJson() != null && !createReq.getMetadataJson().isBlank()) {
            try {
                Map<String, Object> metadata = objectMapper.readValue(
                        createReq.getMetadataJson(),
                        new TypeReference<Map<String, Object>>() {}
                );
                createReq.setMetadata(metadata);
            } catch (Exception e) {
                // JSON 파싱 실패 시 처리 (예: 에러 메시지 반환, 로깅)
                e.printStackTrace(); // 실제로는 로깅 프레임워크 사용
                // model.addAttribute("jsonError", "메타데이터 형식이 잘못되었습니다.");
                // return "courses/course-form";
            }
        }

        // ✅ 3. Spot 이미지 처리 (쉼표 구분 문자열 -> List<String>)
        //    JavaScript에서 이미 List로 처리했다면 이 부분 필요 없음
        //    아래는 input type="text"로 받았을 경우 예시
        if (createReq.getSpots() != null) {
            for (CourseDto.SpotReq spot : createReq.getSpots()) {
                // spot.images 필드가 String이라고 가정. 실제 타입에 맞게 수정 필요
                // if (spot.getImagesString() != null && !spot.getImagesString().isBlank()) {
                //     List<String> images = Arrays.stream(spot.getImagesString().split(","))
                //                             .map(String::trim).filter(img -> !img.isEmpty()).toList();
                //     spot.setImages(images);
                // }
            }
        }


        // 서비스 호출 시에는 원래의 CreateReq 객체를 전달 (서비스는 tags, metadata 필드를 사용)
        courseService.create(createReq, principal.getUsername());
        return "redirect:/home";
    }
}
 */