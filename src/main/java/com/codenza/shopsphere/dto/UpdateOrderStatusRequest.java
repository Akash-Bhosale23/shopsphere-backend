package com.codenza.shopsphere.dto;

import com.codenza.shopsphere.enums.OrderStatus;

import jakarta.validation.constraints.NotNull;

public record UpdateOrderStatusRequest(
        @NotNull(message = "Status is required") OrderStatus status) {
}