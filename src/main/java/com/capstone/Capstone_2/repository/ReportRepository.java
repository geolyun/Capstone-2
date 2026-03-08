package com.capstone.Capstone_2.repository;

import com.capstone.Capstone_2.entity.Report;
import com.capstone.Capstone_2.entity.ReportStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ReportRepository extends JpaRepository<Report, UUID> {

    long countByStatus(ReportStatus status);
}
