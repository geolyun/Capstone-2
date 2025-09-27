package com.capstone.Capstone_2.service;

import com.capstone.Capstone_2.dto.CategoryDto;
import java.util.List;
import java.util.UUID;

public interface CategoryService {
    CategoryDto create(CategoryDto dto);
    CategoryDto update(UUID id, CategoryDto dto);
    void delete(UUID id);
    CategoryDto get(UUID id);
    List<CategoryDto> list();
}
