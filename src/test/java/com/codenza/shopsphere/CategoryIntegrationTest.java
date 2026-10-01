package com.codenza.shopsphere;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.codenza.shopsphere.entity.Category;
import com.codenza.shopsphere.repository.CategoryRepository;

class CategoryIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private CategoryRepository categoryRepository;

    @Test
    void shouldSaveAndRetrieveCategory_usingRealMySqlDatabase() {
        Category category = new Category();
        category.setName("Books");
        category.setDescription("Fiction and non-fiction");

        Category saved = categoryRepository.save(category);

        assertThat(saved.getId()).isNotNull();

        Category found = categoryRepository.findById(saved.getId()).orElseThrow();
        assertThat(found.getName()).isEqualTo("Books");
    }

    @Test
    void shouldRejectDuplicateCategoryName_dueToRealUniqueConstraint() {
        Category first = new Category();
        first.setName("Toys");
        categoryRepository.save(first);

        Category duplicate = new Category();
        duplicate.setName("Toys");

        org.junit.jupiter.api.Assertions.assertThrows(
                org.springframework.dao.DataIntegrityViolationException.class,
                () -> categoryRepository.saveAndFlush(duplicate));
    }
}