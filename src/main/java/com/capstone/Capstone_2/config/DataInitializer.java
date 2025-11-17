package com.capstone.Capstone_2.config;

import com.capstone.Capstone_2.entity.Category;
import com.capstone.Capstone_2.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final CategoryRepository categoryRepository;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        if (categoryRepository.count() == 0) {
            createCategoryGroup("미식", List.of("로컬맛집", "카페투어", "브런치", "디저트", "야식"));
            createCategoryGroup("자연", List.of("산책", "피크닉", "캠핑", "계곡", "정원", "풍경 명소"));
            createCategoryGroup("문화", List.of("전시회", "박물관", "체험공방", "북카페", "공연"));
            createCategoryGroup("활동", List.of("클라이밍", "카약", "볼링", "사격", "실내서핑", "방탈출"));
        }
    }

    private void createCategoryGroup(String rootName, List<String> childrenNames) {
        Category root = Category.builder()
                .name(rootName)
                .slug(rootName)
                .build();
        categoryRepository.save(root);

        for (String childName : childrenNames) {
            Category child = Category.builder()
                    .name(childName)
                    .slug(childName)
                    .parent(root)
                    .build();
            categoryRepository.save(child);
        }
    }
}