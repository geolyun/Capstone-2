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
            createCategoryGroup("테마", List.of("식사", "카페", "산책", "액티비티", "힐링", "여행"));
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