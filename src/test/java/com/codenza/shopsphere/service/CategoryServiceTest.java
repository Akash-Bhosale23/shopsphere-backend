package com.codenza.shopsphere.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.codenza.shopsphere.dto.CategoryRequest;
import com.codenza.shopsphere.dto.CategoryResponse;
import com.codenza.shopsphere.entity.Category;
import com.codenza.shopsphere.exception.DuplicateResourceException;
import com.codenza.shopsphere.exception.ResourceNotFoundException;
import com.codenza.shopsphere.mapper.CategoryMapper;
import com.codenza.shopsphere.repository.CategoryRepository;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private CategoryMapper categoryMapper;

    @InjectMocks
    private CategoryService categoryService;

    private Category category;
    private CategoryRequest request;

    @BeforeEach
    void setUp() {
        category = new Category();
        category.setId(1L);
        category.setName("Electronics");
        category.setDescription("Phones and gadgets");

        request = new CategoryRequest("Electronics", "Phones and gadgets");
    }

    @Test
    void create_shouldSaveCategory_whenNameIsUnique() {
        when(categoryRepository.existsByName("Electronics")).thenReturn(false);
        when(categoryRepository.save(any(Category.class))).thenReturn(category);
        when(categoryMapper.toEntity(request)).thenReturn(category);
        when(categoryMapper.toResponse(category))
                .thenReturn(new CategoryResponse(1L, "Electronics", "Phones and gadgets"));

        CategoryResponse response = categoryService.create(request);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.name()).isEqualTo("Electronics");
        verify(categoryRepository, times(1)).save(any(Category.class));
    }

    @Test
    void create_shouldThrowDuplicateResourceException_whenNameAlreadyExists() {
        when(categoryRepository.existsByName("Electronics")).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> categoryService.create(request));

        verify(categoryRepository, never()).save(any(Category.class));
    }

    @Test
    void getById_shouldReturnCategory_whenFound() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(categoryMapper.toResponse(category))
                .thenReturn(new CategoryResponse(1L, "Electronics", "Phones and gadgets"));

        CategoryResponse response = categoryService.getById(1L);

        assertThat(response.name()).isEqualTo("Electronics");
    }

    @Test
    void getById_shouldThrowResourceNotFoundException_whenNotFound() {
        when(categoryRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> categoryService.getById(99L));
    }

    @Test
    void delete_shouldCallRepositoryDelete_whenCategoryExists() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));

        categoryService.delete(1L);

        verify(categoryRepository, times(1)).delete(category);
    }
}