package com.codenza.shopsphere.mapper;

import org.springframework.stereotype.Component;

import com.codenza.shopsphere.dto.OrderItemResponse;
import com.codenza.shopsphere.dto.OrderResponse;
import com.codenza.shopsphere.entity.Order;
import com.codenza.shopsphere.entity.OrderItem;

@Component
public class OrderMapper {

    public OrderResponse toResponse(Order order, java.util.List<OrderItem> items) {
        java.util.List<OrderItemResponse> itemResponses = items.stream()
                .map(this::toItemResponse)
                .toList();

        return new OrderResponse(
                order.getId(),
                order.getStatus(),
                order.getTotalAmount(),
                order.getCreatedAt(),
                itemResponses);
    }

    private OrderItemResponse toItemResponse(OrderItem item) {
        java.math.BigDecimal subtotal = item.getPriceAtPurchase()
                .multiply(java.math.BigDecimal.valueOf(item.getQuantity()));

        return new OrderItemResponse(
                item.getProduct().getId(),
                item.getProduct().getName(),
                item.getQuantity(),
                item.getPriceAtPurchase(),
                subtotal);
    }
}