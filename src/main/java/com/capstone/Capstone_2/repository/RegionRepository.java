package com.capstone.Capstone_2.repository;

import com.capstone.Capstone_2.entity.Region;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface RegionRepository extends JpaRepository<Region, UUID> {
    // 부모가 없는 최상위 지역(시/도)만 조회 -> Entity Graph 등을 통해 자식도 함께 로딩 가능하지만
    // 여기서는 간단히 최상위 조회 후 DTO 변환 과정에서 자식 접근 (Lazy Loading) 방식을 사용합니다.
    List<Region> findByParentIsNullOrderByCodeAsc();
}
