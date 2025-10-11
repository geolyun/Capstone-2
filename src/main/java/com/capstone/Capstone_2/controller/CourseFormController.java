package com.capstone.Capstone_2.controller;

import com.capstone.Capstone_2.dto.CourseDto;
import com.capstone.Capstone_2.dto.UserPrincipal;
import com.capstone.Capstone_2.service.CourseService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;


@Controller
@RequestMapping("/courses")
@RequiredArgsConstructor
public class CourseFormController {

    private final CourseService courseService;

    // ✅ "/courses/new" GET 요청을 받아 "courses/course-form.html" 페이지를 보여줌
    @GetMapping("/new")
    public String courseForm(Model model) {
        model.addAttribute("course", new CourseDto.CreateReq(null, null, null, null, null, null, null, null, null, null, null));
        return "courses/course-form";
    }

    // ✅ "course-form.html"에서 폼 제출(POST) 시 코스를 생성
    @PostMapping
    public String createCourse(@ModelAttribute("course") CourseDto.CreateReq createReq,
                               @AuthenticationPrincipal UserPrincipal principal) {
        courseService.create(createReq, principal.getUsername());
        return "redirect:/home";
    }
}