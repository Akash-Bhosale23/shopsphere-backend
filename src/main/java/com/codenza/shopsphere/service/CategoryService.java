package com.codenza.shopsphere.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.codenza.shopsphere.dto.CategoryRequest;
import com.codenza.shopsphere.dto.CategoryResponse;
import com.codenza.shopsphere.entity.Category;
import com.codenza.shopsphere.exception.DuplicateResourceException;
import com.codenza.shopsphere.exception.ResourceNotFoundException;
import com.codenza.shopsphere.mapper.CategoryMapper;
import com.codenza.shopsphere.repository.CategoryRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final CategoryMapper categoryMapper;

    @Transactional
    public CategoryResponse create(CategoryRequest request) {
        if (categoryRepository.existsByName(request.name())) {
            throw new DuplicateResourceException("Category already exists with name: " + request.name());
        }
        Category saved = categoryRepository.save(categoryMapper.toEntity(request));
        return categoryMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> getAll() {
        return categoryRepository.findAll().stream()
                .map(categoryMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public CategoryResponse getById(Long id) {
        return categoryMapper.toResponse(findCategory(id));
    }

    @Transactional
    public CategoryResponse update(Long id, CategoryRequest request) {
        Category category = findCategory(id);

        boolean nameChanged = !category.getName().equals(request.name());
        if (nameChanged && categoryRepository.existsByName(request.name())) {
            throw new DuplicateResourceException("Category already exists with name: " + request.name());
        }

        category.setName(request.name());
        category.setDescription(request.description());
        return categoryMapper.toResponse(category);
    }

    @Transactional
    public void delete(Long id) {
        categoryRepository.delete(findCategory(id));
    }

    private Category findCategory(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + id));
    }
}