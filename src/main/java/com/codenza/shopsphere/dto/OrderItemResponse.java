package com.codenza.shopsphere.dto;

import java.math.BigDecimal;

public record OrderItemResponse(
        Long productId,
        String productName,
        Integer quantity,
        BigDecimal priceAtPurchase,
        BigDecimal subtotal) {
}