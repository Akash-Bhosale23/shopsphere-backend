package com.codenza.shopsphere.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.codenza.shopsphere.dto.AddCartItemRequest;
import com.codenza.shopsphere.dto.CartResponse;
import com.codenza.shopsphere.dto.UpdateCartItemRequest;
import com.codenza.shopsphere.entity.Cart;
import com.codenza.shopsphere.entity.CartItem;
import com.codenza.shopsphere.entity.Category;
import com.codenza.shopsphere.entity.Product;
import com.codenza.shopsphere.entity.User;
import com.codenza.shopsphere.enums.Role;
import com.codenza.shopsphere.exception.BadRequestException;
import com.codenza.shopsphere.exception.ResourceNotFoundException;
import com.codenza.shopsphere.mapper.CartMapper;
import com.codenza.shopsphere.repository.CartItemRepository;
import com.codenza.shopsphere.repository.CartRepository;
import com.codenza.shopsphere.repository.ProductRepository;
import com.codenza.shopsphere.security.CurrentUserProvider;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    @Mock
    private CartRepository cartRepository;

    @Mock
    private CartItemRepository cartItemRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CartMapper cartMapper;

    @Mock
    private CurrentUserProvider currentUserProvider;

    @InjectMocks
    private CartService cartService;

    private User customer;
    private Category category;
    private Product product;
    private Cart cart;

    @BeforeEach
    void setUp() {
        customer = new User();
        customer.setId(1L);
        customer.setName("Ravi");
        customer.setEmail("ravi@test.com");
        customer.setRole(Role.CUSTOMER);

        category = new Category();
        category.setId(1L);
        category.setName("Electronics");

        product = new Product();
        product.setId(100L);
        product.setName("Laptop");
        product.setPrice(new BigDecimal("50000"));
        product.setStockQuantity(5);
        product.setCategory(category);

        cart = new Cart();
        cart.setId(1L);
        cart.setUser(customer);

        // These two are used inside getMyCart(), which most CartService
        // methods call internally at the end. Stubbed here with lenient
        // defaults so individual tests only need to override what matters.
    }

    @Test
    void getMyCart_shouldCreateNewCart_whenUserHasNoExistingCart() {
        when(currentUserProvider.getCurrentUser()).thenReturn(customer);
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.empty());
        when(cartRepository.save(any(Cart.class))).thenReturn(cart);
        when(cartItemRepository.findByCartId(1L)).thenReturn(List.of());
        when(cartMapper.toResponse(cart, List.of()))
                .thenReturn(new CartResponse(1L, List.of(), BigDecimal.ZERO));

        CartResponse result = cartService.getMyCart();

        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.items()).isEmpty();

        ArgumentCaptor<Cart> cartCaptor = ArgumentCaptor.forClass(Cart.class);
        verify(cartRepository).save(cartCaptor.capture());
        assertThat(cartCaptor.getValue().getUser()).isEqualTo(customer);
    }

    @Test
    void getMyCart_shouldNotCreateNewCart_whenCartAlreadyExists() {
        when(currentUserProvider.getCurrentUser()).thenReturn(customer);
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCartId(1L)).thenReturn(List.of());
        when(cartMapper.toResponse(cart, List.of()))
                .thenReturn(new CartResponse(1L, List.of(), BigDecimal.ZERO));

        cartService.getMyCart();

        verify(cartRepository, never()).save(any(Cart.class));
    }

    @Test
    void addItem_shouldCreateNewCartItem_whenProductNotAlreadyInCart() {
        AddCartItemRequest request = new AddCartItemRequest(100L, 2);

        when(currentUserProvider.getCurrentUser()).thenReturn(customer);
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));
        when(productRepository.findById(100L)).thenReturn(Optional.of(product));
        when(cartItemRepository.findByCartIdAndProductId(1L, 100L)).thenReturn(Optional.empty());

        // Stubs for the internal getMyCart() call at the end of addItem()
        when(cartItemRepository.findByCartId(1L)).thenReturn(List.of());
        when(cartMapper.toResponse(cart, List.of()))
                .thenReturn(new CartResponse(1L, List.of(), BigDecimal.ZERO));

        cartService.addItem(request);

        ArgumentCaptor<CartItem> itemCaptor = ArgumentCaptor.forClass(CartItem.class);
        verify(cartItemRepository).save(itemCaptor.capture());

        CartItem savedItem = itemCaptor.getValue();
        assertThat(savedItem.getQuantity()).isEqualTo(2);
        assertThat(savedItem.getProduct()).isEqualTo(product);
        assertThat(savedItem.getCart()).isEqualTo(cart);
    }

    @Test
    void addItem_shouldIncreaseExistingQuantity_whenProductAlreadyInCart() {
        AddCartItemRequest request = new AddCartItemRequest(100L, 2);

        CartItem existingItem = new CartItem();
        existingItem.setId(500L);
        existingItem.setCart(cart);
        existingItem.setProduct(product);
        existingItem.setQuantity(1);

        when(currentUserProvider.getCurrentUser()).thenReturn(customer);
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));
        when(productRepository.findById(100L)).thenReturn(Optional.of(product));
        when(cartItemRepository.findByCartIdAndProductId(1L, 100L)).thenReturn(Optional.of(existingItem));

        when(cartItemRepository.findByCartId(1L)).thenReturn(List.of(existingItem));
        when(cartMapper.toResponse(cart, List.of(existingItem)))
                .thenReturn(new CartResponse(1L, List.of(), new BigDecimal("150000")));

        cartService.addItem(request);

        ArgumentCaptor<CartItem> itemCaptor = ArgumentCaptor.forClass(CartItem.class);
        verify(cartItemRepository).save(itemCaptor.capture());

        // 1 (already in cart) + 2 (this request) = 3
        assertThat(itemCaptor.getValue().getQuantity()).isEqualTo(3);
        assertThat(itemCaptor.getValue().getId()).isEqualTo(500L);
    }

    @Test
    void addItem_shouldThrowBadRequestException_whenRequestedQuantityExceedsStock() {
        product.setStockQuantity(2);
        AddCartItemRequest request = new AddCartItemRequest(100L, 5);

        when(currentUserProvider.getCurrentUser()).thenReturn(customer);
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));
        when(productRepository.findById(100L)).thenReturn(Optional.of(product));
        when(cartItemRepository.findByCartIdAndProductId(1L, 100L)).thenReturn(Optional.empty());

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> cartService.addItem(request));

        assertThat(exception.getMessage()).contains("Only 2 unit(s)").contains("Laptop");
        verify(cartItemRepository, never()).save(any(CartItem.class));
    }

    @Test
    void addItem_shouldThrowResourceNotFoundException_whenProductDoesNotExist() {
        AddCartItemRequest request = new AddCartItemRequest(999L, 1);

        when(currentUserProvider.getCurrentUser()).thenReturn(customer);
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));
        when(productRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> cartService.addItem(request));

        verify(cartItemRepository, never()).save(any(CartItem.class));
    }

    @Test
    void updateItemQuantity_shouldUpdateQuantity_whenItemExists() {
        CartItem existingItem = new CartItem();
        existingItem.setId(500L);
        existingItem.setCart(cart);
        existingItem.setProduct(product);
        existingItem.setQuantity(1);

        UpdateCartItemRequest request = new UpdateCartItemRequest(4);

        when(currentUserProvider.getCurrentUser()).thenReturn(customer);
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCartIdAndProductId(1L, 100L)).thenReturn(Optional.of(existingItem));

        when(cartItemRepository.findByCartId(1L)).thenReturn(List.of(existingItem));
        when(cartMapper.toResponse(cart, List.of(existingItem)))
                .thenReturn(new CartResponse(1L, List.of(), new BigDecimal("200000")));

        cartService.updateItemQuantity(100L, request);

        assertThat(existingItem.getQuantity()).isEqualTo(4);
    }

    @Test
    void removeItem_shouldThrowResourceNotFoundException_whenProductNotInCart() {
        when(currentUserProvider.getCurrentUser()).thenReturn(customer);
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCartIdAndProductId(1L, 100L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> cartService.removeItem(100L));

        verify(cartItemRepository, never()).delete(any(CartItem.class));
    }

    @Test
    void removeItem_shouldDeleteItem_whenItExists() {
        CartItem existingItem = new CartItem();
        existingItem.setId(500L);
        existingItem.setCart(cart);
        existingItem.setProduct(product);
        existingItem.setQuantity(2);

        when(currentUserProvider.getCurrentUser()).thenReturn(customer);
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCartIdAndProductId(1L, 100L)).thenReturn(Optional.of(existingItem));

        when(cartItemRepository.findByCartId(1L)).thenReturn(List.of());
        when(cartMapper.toResponse(cart, List.of()))
                .thenReturn(new CartResponse(1L, List.of(), BigDecimal.ZERO));

        cartService.removeItem(100L);

        verify(cartItemRepository, times(1)).delete(existingItem);
    }

    @Test
    void clearCart_shouldDeleteAllItems() {
        CartItem item1 = new CartItem();
        item1.setId(500L);
        CartItem item2 = new CartItem();
        item2.setId(501L);
        List<CartItem> items = List.of(item1, item2);

        when(currentUserProvider.getCurrentUser()).thenReturn(customer);
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCartId(1L)).thenReturn(items);

        cartService.clearCart();

        verify(cartItemRepository).deleteAll(items);
    }
}