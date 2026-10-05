package com.codenza.shopsphere.event;

import java.io.Serializable;
import java.math.BigDecimal;

public record OrderPlacedEvent(
        Long orderId,
        String customerEmail,
        BigDecimal totalAmount) implements Serializable {
}