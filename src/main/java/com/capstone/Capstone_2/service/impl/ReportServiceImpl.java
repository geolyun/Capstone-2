package com.capstone.Capstone_2.service.impl;

import com.capstone.Capstone_2.dto.ReportDto;
import com.capstone.Capstone_2.entity.Course;
import com.capstone.Capstone_2.entity.Report;
import com.capstone.Capstone_2.entity.User;
import com.capstone.Capstone_2.repository.CourseRepository;
import com.capstone.Capstone_2.repository.ReportRepository;
import com.capstone.Capstone_2.repository.UserRepository;
import com.capstone.Capstone_2.service.ReportService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {
    private final ReportRepository reportRepository;
    private final UserRepository userRepository;
    private final CourseRepository courseRepository;

    @Override
    @Transactional
    public void createReport(UUID courseId, String reporterEmail, ReportDto.CreateRequest request) {
        User reporter = userRepository.findByEmail(reporterEmail).orElseThrow(() -> new EntityNotFoundException("User not found"));
        Course course = courseRepository.findById(courseId).orElseThrow(() -> new EntityNotFoundException("Course not found"));

        Report report = Report.builder()
                .reporter(reporter)
                .reportedCourse(course)
                .reason(request.getReason())
                .description(request.getDescription())
                .build();

        reportRepository.save(report);
    }
}