package com.codenza.shopsphere.mapper;

import org.springframework.stereotype.Component;

import com.codenza.shopsphere.dto.CategoryRequest;
import com.codenza.shopsphere.dto.CategoryResponse;
import com.codenza.shopsphere.entity.Category;

@Component
public class CategoryMapper {

    public Category toEntity(CategoryRequest request) {
        Category category = new Category();
        category.setName(request.name());
        category.setDescription(request.description());
        return category;
    }

    public CategoryResponse toResponse(Category category) {
        return new CategoryResponse(category.getId(), category.getName(), category.getDescription());
    }
}