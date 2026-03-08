package com.capstone.Capstone_2.repository;

import com.capstone.Capstone_2.entity.Region;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface RegionRepository extends JpaRepository<Region, UUID> {
    // 부모가 없는 최상위 지역(시/도)만 조회
    List<Region> findByParentIsNullOrderByCodeAsc();
}
