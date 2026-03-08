package com.capstone.Capstone_2.controller;

import com.capstone.Capstone_2.dto.CategoryDto;
import com.capstone.Capstone_2.service.course.CategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService service;

    @PostMapping
    public ResponseEntity<CategoryDto> create(@Valid @RequestBody CategoryDto dto) {
        return ResponseEntity.status(201).body(service.create(dto));
    }

    @GetMapping("/{id}")
    public CategoryDto get(@PathVariable UUID id) {
        return service.get(id);
    }

    @PutMapping("/{id}")
    public CategoryDto update(@PathVariable UUID id, @RequestBody CategoryDto dto) {
        return service.update(id, dto);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public List<CategoryDto> list() {
        return service.list();
    }
}