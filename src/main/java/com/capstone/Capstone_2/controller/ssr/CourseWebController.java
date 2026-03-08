package com.capstone.Capstone_2.controller.ssr;

import com.capstone.Capstone_2.config.security.UserPrincipal;
import com.capstone.Capstone_2.dto.*;
import com.capstone.Capstone_2.entity.CreatorProfile;
import com.capstone.Capstone_2.repository.RegionRepository;
import com.capstone.Capstone_2.service.course.CategoryService;
import com.capstone.Capstone_2.service.course.CourseService;
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

// 서버사이드 렌더링 전용 컨트롤러
@Controller
@RequestMapping("/courses")
@RequiredArgsConstructor
public class CourseWebController {

    private final CourseService courseService;
    private final CategoryService categoryService;
    private final RegionRepository regionRepository;
    private static final Logger logger = LoggerFactory.getLogger(CourseWebController.class);

    @GetMapping
    public String courseListPage(
            @ModelAttribute("searchDto") CourseSearchDto searchDto,
            @PageableDefault(size = 9, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            Model model) {

        Page<CourseDto.CourseSummary> coursePage;

        // sortType이 popular면 인기 코스 출력
        if ("popular".equalsIgnoreCase(searchDto.getSortType())) {
            coursePage = courseService.getPopularCourses(pageable);
        } else {
            coursePage = courseService.search(searchDto, pageable);
        }

        model.addAttribute("coursePage", coursePage);
        model.addAttribute("searchDto", searchDto);

        return "courses/list";
    }

    @GetMapping("/new")
    public String courseForm(Model model) {
        model.addAttribute("course", new CourseDto.CreateReq());

        // 2. 카테고리 목록을 계층형 Map으로 변환하여 모델에 추가
        model.addAttribute("categoryMap", getGroupedCategories());

        List<RegionDto> regions = regionRepository.findByParentIsNullOrderByCodeAsc()
                .stream()
                .map(RegionDto::new)
                .toList();
        model.addAttribute("regions", regions);

        return "courses/course-form";
    }

    @PostMapping
    // "/courses" 경로의 POST 요청 처리
    public String createCourse(
            @Valid @ModelAttribute("course") CourseDto.CreateReq createReq,
            BindingResult bindingResult,
            @AuthenticationPrincipal UserPrincipal principal,
            Model model) {

        logger.info("Received course creation request via web form: {}", createReq.getTitle());

        if (bindingResult.hasErrors()) {
            logger.warn("Validation errors on course form: {}", bindingResult.getAllErrors());
            return "courses/course-form";
        }

        try {
            // tagsString 파싱
            if (createReq.getTagsString() != null && !createReq.getTagsString().isBlank()) {
                List<String> tags = Arrays.stream(createReq.getTagsString().split(","))
                        .map(String::trim).filter(tag -> !tag.isEmpty()).toList();
                createReq.setTags(tags);
            }

            // 서비스 호출
            courseService.create(createReq, principal.getUsername());
            logger.info("Course created successfully via web form!");
            return "redirect:/";

        } catch (Exception e) {
            logger.error("Error creating course via web form: {}", e.getMessage(), e);
            model.addAttribute("errorMessage", "코스 생성 중 오류 발생: " + e.getMessage());
            model.addAttribute("course", createReq);
            return "courses/course-form";
        }
    }

    @GetMapping("/{courseId}/recommendations-page")
    public String showRecommendationsPage(@PathVariable UUID courseId, Model model, @AuthenticationPrincipal UserPrincipal principal) {
        String email = (principal != null) ? principal.getUsername(): null;
        RecommendationDto recommendations = courseService.getCourseRecommendations(courseId);
        CourseDto.Detail sourceCourse = courseService.get(courseId, email);

        model.addAttribute("sourceCourse", sourceCourse);
        model.addAttribute("recommendations", recommendations);

        return "courses/recommendations";
    }

    @GetMapping("/{courseId}")
    public String courseDetailPage(@PathVariable UUID courseId, Model model, @AuthenticationPrincipal UserPrincipal principal) {
        try {
            String email = (principal != null) ? principal.getUsername() : null;
            CourseDto.Detail courseDetail = courseService.get(courseId, email);
            model.addAttribute("course", courseDetail);
            return "courses/detail";
        } catch (Exception e) {
           return "error/404";
        }
    }

    @GetMapping("/{courseId}/edit")
    public String courseEditForm(@PathVariable UUID courseId, Model model, @AuthenticationPrincipal UserPrincipal principal) {
        try {
            String email = (principal != null) ? principal.getUsername() : null;
            CourseDto.Detail courseDetail = courseService.get(courseId, email);

            CreatorProfile creatorProfile = principal != null ? principal.getUser().getCreatorProfile() : null;
            if (creatorProfile == null || !courseDetail.creatorId().equals(creatorProfile.getId())) {
                throw new AccessDeniedException("수정 권한이 없습니다.");
            }

            CourseDto.UpdateReq updateReq = mapDetailToUpdateReq(courseDetail);

            model.addAttribute("course", updateReq);
            model.addAttribute("courseId", courseId);

            model.addAttribute("categoryMap", getGroupedCategories());

            List<RegionDto> regions = regionRepository.findByParentIsNullOrderByCodeAsc()
                    .stream()
                    .map(RegionDto::new)
                    .toList();
            model.addAttribute("regions", regions);

            return "courses/course-edit-form";
        } catch (EntityNotFoundException e) {
            return "error/404";
        } catch (AccessDeniedException e) {
            return "redirect:/courses/" + courseId + "?error=permission_denied";
        }
    }

    private Map<String, List<CategoryDto>> getGroupedCategories() {
        List<CategoryDto> all = categoryService.list();
        Map<String, List<CategoryDto>> map = new LinkedHashMap<>();

        // 대분류(Parent가 null인 것)를 먼저 찾음
        List<CategoryDto> roots = all.stream()
                .filter(c -> c.parentId() == null)
                .toList();

        for (CategoryDto root : roots) {
            // 해당 대분류에 속한 소분류들을 찾음
            List<CategoryDto> children = all.stream()
                    .filter(c -> root.id().equals(c.parentId()))
                    .toList();
            if (!children.isEmpty()) {
                map.put(root.name(), children);
            }
        }
        return map;
    }

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
            model.addAttribute("courseId", courseId);
            return "courses/course-edit-form";
        }

        try {
            // (Service의 update 메서드에서 tagsString/metadataJson 파싱이 처리됨)
            courseService.update(courseId, updateReq, principal.getUsername());
            logger.info("Course updated successfully via web form!");
            return "redirect:/courses/" + courseId;

        } catch (EntityNotFoundException e) {
            logger.error("Error updating course: {}", e.getMessage(), e);
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("courseId", courseId);
            return "courses/course-edit-form";
        } catch (AccessDeniedException e) {
            logger.warn("Access denied during course update: {}", e.getMessage());
            return "redirect:/courses/" + courseId + "?error=permission_denied";
        } catch (Exception e) {
            logger.error("Unexpected error updating course: {}", e.getMessage(), e);
            model.addAttribute("errorMessage", "코스 수정 중 오류 발생: " + e.getMessage());
            model.addAttribute("courseId", courseId);
            model.addAttribute("course", updateReq);
            return "courses/course-edit-form";
        }
    }

    private CourseDto.UpdateReq mapDetailToUpdateReq(CourseDto.Detail detail) {
        CourseDto.UpdateReq updateReq = new CourseDto.UpdateReq();
        updateReq.setTitle(detail.title());
        updateReq.setSummary(detail.summary());
        updateReq.setCoverImageUrl(detail.coverImageUrl());
        updateReq.setRegionCode(detail.regionCode());
        updateReq.setRegionName(detail.regionName());
        updateReq.setDurationMinutes(detail.durationMinutes());
        updateReq.setEstimatedCost(detail.estimatedCost());
        updateReq.setTags(new ArrayList<>(detail.tags()));
        updateReq.setTagsString(String.join(", ", detail.tags()));

        // SpotRes -> SpotReq 변환
        if (detail.spots() != null) {
            List<CourseDto.SpotReq> spotReqs = detail.spots().stream().map(spotRes -> {
                CourseDto.SpotReq spotReq = new CourseDto.SpotReq();
                spotReq.setOrderNo(spotRes.orderNo());
                spotReq.setTitle(spotRes.title());
                spotReq.setDescription(spotRes.description());
                spotReq.setLat(spotRes.lat());
                spotReq.setLng(spotRes.lng());
                spotReq.setImages(new ArrayList<>(spotRes.images()));
                spotReq.setStayMinutes(spotRes.stayMinutes());
                spotReq.setPrice(spotRes.price());
                return spotReq;
            }).collect(Collectors.toList());
            updateReq.setSpots(spotReqs);
        }

        return updateReq;
    }

    @PostMapping("/{courseId}/delete")
    public String deleteCourse(@PathVariable UUID courseId, @AuthenticationPrincipal UserPrincipal principal, RedirectAttributes redirectAttributes) {
        logger.info("Received course delete request via web form for course ID: {}", courseId);
        try {
            courseService.delete(courseId, principal.getUsername());
            logger.info("Course deleted successfully: {}", courseId);
            redirectAttributes.addFlashAttribute("successMessage", "코스가 성공적으로 삭제되었습니다.");
            return "redirect:/courses";
        } catch (EntityNotFoundException e) {
            logger.warn("Attempted to delete non-existent course: {}", courseId);
            redirectAttributes.addFlashAttribute("errorMessage", "삭제하려는 코스를 찾을 수 없습니다.");
            return "redirect:/courses";
        } catch (AccessDeniedException e) {
            logger.warn("Access denied during course deletion attempt for course ID: {}", courseId);
            redirectAttributes.addFlashAttribute("errorMessage", "코스를 삭제할 권한이 없습니다.");
            return "redirect:/courses/" + courseId;
        } catch (Exception e) {
            logger.error("Error deleting course {}: {}", courseId, e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", "코스 삭제 중 오류가 발생했습니다.");
            return "redirect:/courses/" + courseId;
        }
    }

}