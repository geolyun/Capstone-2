package com.capstone.Capstone_2.service.impl;

import com.capstone.Capstone_2.entity.Category;
import com.capstone.Capstone_2.dto.CategoryDto;
import com.capstone.Capstone_2.repository.CategoryRepository;
import com.capstone.Capstone_2.service.CategoryService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.util.List;
import java.util.UUID;


@Service
@RequiredArgsConstructor
@Transactional
public class CategoryServiceImpl implements CategoryService {
    private final CategoryRepository repo;


    @Override
    public CategoryDto create(CategoryDto dto) {
        Category c = new Category();
        c.setName(dto.name());
        c.setSlug(dto.slug());
        if (dto.parentId() != null) {
            c.setParent(repo.findById(dto.parentId()).orElseThrow());
        }
        repo.save(c);
        return new CategoryDto(c.getId(), c.getName(), c.getSlug(), c.getParent() == null ? null : c.getParent().getId());
    }


    @Override
    public CategoryDto update(UUID id, CategoryDto dto) {
        Category c = repo.findById(id).orElseThrow(EntityNotFoundException::new);
        if (dto.name() != null) c.setName(dto.name());
        if (dto.slug() != null) c.setSlug(dto.slug());
        if (dto.parentId() != null) c.setParent(repo.findById(dto.parentId()).orElseThrow());
        return new CategoryDto(c.getId(), c.getName(), c.getSlug(), c.getParent() == null ? null : c.getParent().getId());
    }


    @Override
    public void delete(UUID id) {
        repo.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public CategoryDto get(UUID id) {
        Category c = repo.findById(id).orElseThrow(EntityNotFoundException::new);
        return new CategoryDto(c.getId(), c.getName(), c.getSlug(), c.getParent() == null ? null : c.getParent().getId());
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryDto> list() {
        return repo.findAll().stream().map(c -> new CategoryDto(c.getId(), c.getName(), c.getSlug(), c.getParent() == null ? null : c.getParent().getId())).toList();
    }
}