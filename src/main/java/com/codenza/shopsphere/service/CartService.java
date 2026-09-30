package com.codenza.shopsphere.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.codenza.shopsphere.dto.AddCartItemRequest;
import com.codenza.shopsphere.dto.CartResponse;
import com.codenza.shopsphere.dto.UpdateCartItemRequest;
import com.codenza.shopsphere.entity.Cart;
import com.codenza.shopsphere.entity.CartItem;
import com.codenza.shopsphere.entity.Product;
import com.codenza.shopsphere.entity.User;
import com.codenza.shopsphere.exception.BadRequestException;
import com.codenza.shopsphere.exception.ResourceNotFoundException;
import com.codenza.shopsphere.mapper.CartMapper;
import com.codenza.shopsphere.repository.CartItemRepository;
import com.codenza.shopsphere.repository.CartRepository;
import com.codenza.shopsphere.repository.ProductRepository;
import com.codenza.shopsphere.security.CurrentUserProvider;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final CartMapper cartMapper;
    private final CurrentUserProvider currentUserProvider;

    @Transactional
    public CartResponse getMyCart() {
        Cart cart = getOrCreateCart();
        List<CartItem> items = cartItemRepository.findByCartId(cart.getId());
        return cartMapper.toResponse(cart, items);
    }

    @Transactional
    public CartResponse addItem(AddCartItemRequest request) {
        Cart cart = getOrCreateCart();

        Product product = productRepository.findById(request.productId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Product not found with id: " + request.productId()));

        CartItem item = cartItemRepository.findByCartIdAndProductId(cart.getId(), product.getId())
                .orElseGet(() -> {
                    CartItem newItem = new CartItem();
                    newItem.setCart(cart);
                    newItem.setProduct(product);
                    newItem.setQuantity(0);
                    return newItem;
                });

        int newQuantity = item.getQuantity() + request.quantity();
        assertStockAvailable(product, newQuantity);
        item.setQuantity(newQuantity);
        cartItemRepository.save(item);

        return getMyCart();
    }

    @Transactional
    public CartResponse updateItemQuantity(Long productId, UpdateCartItemRequest request) {
        Cart cart = getOrCreateCart();
        CartItem item = cartItemRepository.findByCartIdAndProductId(cart.getId(), productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not in cart: " + productId));

        assertStockAvailable(item.getProduct(), request.quantity());
        item.setQuantity(request.quantity());

        return getMyCart();
    }

    @Transactional
    public CartResponse removeItem(Long productId) {
        Cart cart = getOrCreateCart();
        CartItem item = cartItemRepository.findByCartIdAndProductId(cart.getId(), productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not in cart: " + productId));

        cartItemRepository.delete(item);
        return getMyCart();
    }

    @Transactional
    public void clearCart() {
        Cart cart = getOrCreateCart();
        List<CartItem> items = cartItemRepository.findByCartId(cart.getId());
        cartItemRepository.deleteAll(items);
    }

    private Cart getOrCreateCart() {
        User currentUser = currentUserProvider.getCurrentUser();
        return cartRepository.findByUserId(currentUser.getId())
                .orElseGet(() -> {
                    Cart newCart = new Cart();
                    newCart.setUser(currentUser);
                    return cartRepository.save(newCart);
                });
    }

    private void assertStockAvailable(Product product, int requestedQuantity) {
        if (requestedQuantity > product.getStockQuantity()) {
            throw new BadRequestException(
                    "Only " + product.getStockQuantity() + " unit(s) of \"" + product.getName()
                            + "\" available in stock");
        }
    }
}