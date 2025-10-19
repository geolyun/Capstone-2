package com.capstone.Capstone_2.repository;

import com.capstone.Capstone_2.entity.Report;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface ReportRepository extends JpaRepository<Report, UUID> {

    Page<Report> findByStatus(Report.ReportStatus status, Pageable pageable);

    long countByStatus(Report.ReportStatus status);
}