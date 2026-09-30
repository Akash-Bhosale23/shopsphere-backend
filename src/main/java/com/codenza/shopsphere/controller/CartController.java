package com.codenza.shopsphere.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.codenza.shopsphere.dto.AddCartItemRequest;
import com.codenza.shopsphere.dto.CartResponse;
import com.codenza.shopsphere.dto.UpdateCartItemRequest;
import com.codenza.shopsphere.service.CartService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "Cart", description = "Manage the logged-in customer's shopping cart")
@RestController
@RequestMapping("/api/v1/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    @Operation(summary = "View my cart")
    @GetMapping
    public CartResponse getMyCart() {
        return cartService.getMyCart();
    }

    @Operation(summary = "Add a product to my cart")
    @PostMapping("/items")
    public CartResponse addItem(@Valid @RequestBody AddCartItemRequest request) {
        return cartService.addItem(request);
    }

    @Operation(summary = "Set the quantity of a product already in my cart")
    @PutMapping("/items/{productId}")
    public CartResponse updateItem(@PathVariable Long productId,
                                    @Valid @RequestBody UpdateCartItemRequest request) {
        return cartService.updateItemQuantity(productId, request);
    }

    @Operation(summary = "Remove a product from my cart")
    @DeleteMapping("/items/{productId}")
    public CartResponse removeItem(@PathVariable Long productId) {
        return cartService.removeItem(productId);
    }

    @Operation(summary = "Empty my cart")
    @DeleteMapping
    public ResponseEntity<Void> clearCart() {
        cartService.clearCart();
        return ResponseEntity.noContent().build();
    }
}