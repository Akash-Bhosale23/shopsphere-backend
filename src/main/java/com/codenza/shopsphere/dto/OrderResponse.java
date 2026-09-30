package com.codenza.shopsphere.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.codenza.shopsphere.enums.OrderStatus;

public record OrderResponse(
        Long id,
        OrderStatus status,
        BigDecimal totalAmount,
        LocalDateTime createdAt,
        List<OrderItemResponse> items) {
}