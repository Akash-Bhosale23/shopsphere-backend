package com.codenza.shopsphere.repository.spec;

import java.math.BigDecimal;

import org.springframework.data.jpa.domain.Specification;

import com.codenza.shopsphere.entity.Product;

public class ProductSpecifications {

    private ProductSpecifications() {
    }

    public static Specification<Product> hasCategoryId(Long categoryId) {
        return (root, query, cb) -> categoryId == null ? null
                : cb.equal(root.get("category").get("id"), categoryId);
    }

    public static Specification<Product> nameContains(String keyword) {
        return (root, query, cb) -> (keyword == null || keyword.isBlank()) ? null
                : cb.like(cb.lower(root.get("name")), "%" + keyword.toLowerCase() + "%");
    }

    public static Specification<Product> priceGreaterOrEqual(BigDecimal min) {
        return (root, query, cb) -> min == null ? null
                : cb.greaterThanOrEqualTo(root.get("price"), min);
    }

    public static Specification<Product> priceLessOrEqual(BigDecimal max) {
        return (root, query, cb) -> max == null ? null
                : cb.lessThanOrEqualTo(root.get("price"), max);
    }
}