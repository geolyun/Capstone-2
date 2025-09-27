package com.capstone.Capstone_2.controller;

import com.capstone.Capstone_2.dto.CategoryDto;
import com.capstone.Capstone_2.service.CategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;


@RestController @RequestMapping("/api/categories") @RequiredArgsConstructor
public class CategoryController {
    private final CategoryService service;


    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    public CategoryDto create(@Valid @RequestBody CategoryDto dto) { return service.create(dto); }


    @PutMapping("/{id}")
    public CategoryDto update(@PathVariable UUID id, @RequestBody CategoryDto dto) { return service.update(id, dto); }


    @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) { service.delete(id); }


    @GetMapping("/{id}") public CategoryDto get(@PathVariable UUID id) { return service.get(id); }


    @GetMapping public List<CategoryDto> list() { return service.list(); }
}