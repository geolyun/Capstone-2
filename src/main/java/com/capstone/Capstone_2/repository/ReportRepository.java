package com.capstone.Capstone_2.repository;

import com.capstone.Capstone_2.entity.Report;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface ReportRepository extends JpaRepository<Report, UUID> {

    // ✅ 관리자 대시보드에서 특정 상태(예: PENDING)의 신고 목록을
    //    페이징하여 조회하기 위해 이 메서드가 필요합니다.
    Page<Report> findByStatus(Report.ReportStatus status, Pageable pageable);

}