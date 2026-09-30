package com.codenza.shopsphere.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.codenza.shopsphere.dto.OrderResponse;
import com.codenza.shopsphere.dto.UpdateOrderStatusRequest;
import com.codenza.shopsphere.service.OrderService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "Orders", description = "Place and manage orders")
@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @Operation(summary = "Place an order from my cart")
    @PostMapping
    public ResponseEntity<OrderResponse> placeOrder() {
        return ResponseEntity.status(HttpStatus.CREATED).body(orderService.placeOrder());
    }

    @Operation(summary = "View my order history")
    @GetMapping
    public List<OrderResponse> getMyOrders() {
        return orderService.getMyOrders();
    }

    @Operation(summary = "View a specific order")
    @GetMapping("/{id}")
    public OrderResponse getById(@PathVariable Long id) {
        return orderService.getById(id);
    }

    @Operation(summary = "Cancel my order")
    @PostMapping("/{id}/cancel")
    public ResponseEntity<Void> cancel(@PathVariable Long id) {
        orderService.cancelOrder(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Update order status (admin only)")
    @PutMapping("/{id}/status")
    public OrderResponse updateStatus(@PathVariable Long id, @Valid @RequestBody UpdateOrderStatusRequest request) {
        return orderService.updateStatus(id, request);
    }
}