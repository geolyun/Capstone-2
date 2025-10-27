package com.capstone.Capstone_2.repository;

import com.capstone.Capstone_2.dto.CourseSearchDto;
import com.capstone.Capstone_2.entity.Course;
import com.capstone.Capstone_2.entity.QCourse;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Order; // QueryDSL Order import
import com.querydsl.core.types.OrderSpecifier; // QueryDSL OrderSpecifier import
import com.querydsl.core.types.dsl.ComparablePath;
import com.querydsl.core.types.dsl.PathBuilder; // PathBuilder import
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort; // Spring Sort import
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.util.StringUtils;

import java.util.List;

@RequiredArgsConstructor // 생성자 주입
public class CourseRepositoryImpl implements CourseRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public Page<Course> searchByFilter(CourseSearchDto searchDto, Pageable pageable) {
        QCourse course = QCourse.course;
        BooleanBuilder builder = new BooleanBuilder();

        // --- 동적 쿼리 조건 생성 ---

        // 1. 텍스트 검색 (제목 또는 요약)
        if (StringUtils.hasText(searchDto.getQ())) {
            builder.and(course.title.containsIgnoreCase(searchDto.getQ())
                    .or(course.summary.containsIgnoreCase(searchDto.getQ()))
            );
        }

        // 2. 지역 코드 필터 -> regionCode 필드 사용 (CourseSearchDto에 regionCode 필드가 있다고 가정)
        // if (StringUtils.hasText(searchDto.getRegion())) { // DTO 필드명 확인 필요
        //     builder.and(course.regionCode.eq(searchDto.getRegion()));
        // }
        // 수정: DTO에 region 필드가 regionCode를 의미한다고 가정
        if (StringUtils.hasText(searchDto.getRegion())) {
            builder.and(course.regionCode.eq(searchDto.getRegion()));
        }


        // 3. 최대 예산 필터
        if (searchDto.getMaxCost() != null) {
            builder.and(course.estimatedCost.loe(searchDto.getMaxCost()));
        }

        // 4. 최대 소요 시간 필터
        if (searchDto.getMaxDuration() != null) {
            builder.and(course.durationMinutes.loe(searchDto.getMaxDuration()));
        }

        // 5. 태그 필터 (태그 목록(Set)에 포함되어 있는지)
        if (StringUtils.hasText(searchDto.getTag())) {
            builder.and(course.tags.contains(searchDto.getTag()));
        }

        // 6. 승인된 코스만 (기본 조건) - 필요 시 추가
        // builder.and(course.reviewState.eq(ReviewState.APPROVED));

        // --- 쿼리 실행 ---

        // 1. 데이터 조회 쿼리 (페이징 적용)
        JPAQuery<Course> query = queryFactory
                .selectFrom(course)
                .where(builder)
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize());

        // --- 정렬(Sort) 적용 ---
        for (Sort.Order order : pageable.getSort()) {
            PathBuilder<Course> pathBuilder = new PathBuilder<>(Course.class, "course");
            // getComparable 메서드를 사용하여 ComparablePath<?> 타입으로 받음
            ComparablePath<?> sortPropertyExpression = pathBuilder.getComparable(order.getProperty(), Comparable.class);

            query.orderBy(new OrderSpecifier<>(
                    order.isAscending() ? Order.ASC : Order.DESC,
                    sortPropertyExpression // ComparablePath 사용
            ));
        }


        List<Course> content = query.fetch();

        // 2. 전체 카운트 조회 쿼리 (페이징을 위해 필요)
        // countQuery 최적화: fetchCount() 대신 count() 사용 권장, QueryDSL 5.x 기준
        JPAQuery<Long> countQuery = queryFactory
                .select(course.count())
                .from(course)
                .where(builder);

        // 3. Page 객체로 변환하여 반환
        // fetchOne()은 결과가 null일 수 있으므로 Optional 처리 또는 기본값 처리 권장
        // return PageableExecutionUtils.getPage(content, pageable, countQuery::fetchOne);
        return PageableExecutionUtils.getPage(content, pageable, () -> {
            Long total = countQuery.fetchOne();
            return total == null ? 0L : total;
        });
    }
}