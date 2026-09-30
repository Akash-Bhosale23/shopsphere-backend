package com.codenza.shopsphere.dto;

import java.math.BigDecimal;

public record ProductResponse(
        Long id,
        String name,
        String description,
        BigDecimal price,
        Integer stockQuantity,
        CategorySummary category,
        SellerSummary seller) {
}