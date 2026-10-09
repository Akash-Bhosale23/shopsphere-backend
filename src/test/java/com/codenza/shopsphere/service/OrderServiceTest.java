package com.codenza.shopsphere.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import com.codenza.shopsphere.config.RabbitMQConfig;
import com.codenza.shopsphere.event.OrderPlacedEvent;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.OptimisticLockingFailureException;

import com.codenza.shopsphere.dto.OrderResponse;
import com.codenza.shopsphere.dto.UpdateOrderStatusRequest;
import com.codenza.shopsphere.entity.Cart;
import com.codenza.shopsphere.entity.CartItem;
import com.codenza.shopsphere.entity.Category;
import com.codenza.shopsphere.entity.Order;
import com.codenza.shopsphere.entity.OrderItem;
import com.codenza.shopsphere.entity.Product;
import com.codenza.shopsphere.entity.User;
import com.codenza.shopsphere.enums.OrderStatus;
import com.codenza.shopsphere.enums.Role;
import com.codenza.shopsphere.exception.BadRequestException;
import com.codenza.shopsphere.exception.ResourceNotFoundException;
import com.codenza.shopsphere.exception.UnauthorizedActionException;
import com.codenza.shopsphere.mapper.OrderMapper;
import com.codenza.shopsphere.repository.CartItemRepository;
import com.codenza.shopsphere.repository.CartRepository;
import com.codenza.shopsphere.repository.OrderItemRepository;
import com.codenza.shopsphere.repository.OrderRepository;
import com.codenza.shopsphere.repository.ProductRepository;
import com.codenza.shopsphere.security.CurrentUserProvider;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.mockito.Spy;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    @Mock
    private CartRepository cartRepository;

    @Mock
    private CartItemRepository cartItemRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private OrderMapper orderMapper;

    @Mock
    private CurrentUserProvider currentUserProvider;
    
    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private OrderService orderService;
    
    @Spy
    private MeterRegistry meterRegistry = new SimpleMeterRegistry();

    private User customer;
    private User admin;
    private Category category;
    private Product product;
    private Cart cart;
    private CartItem cartItem;
    private Order order;

    @BeforeEach
    void setUp() {
        customer = new User();
        customer.setId(1L);
        customer.setName("Ravi");
        customer.setEmail("ravi@test.com");
        customer.setRole(Role.CUSTOMER);

        admin = new User();
        admin.setId(2L);
        admin.setName("Admin");
        admin.setEmail("admin@test.com");
        admin.setRole(Role.ADMIN);

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

        cartItem = new CartItem();
        cartItem.setId(500L);
        cartItem.setCart(cart);
        cartItem.setProduct(product);
        cartItem.setQuantity(2);

        order = new Order();
        order.setId(1000L);
        order.setUser(customer);
        order.setStatus(OrderStatus.PLACED);
        order.setTotalAmount(new BigDecimal("100000"));
    }

    @Test
    void placeOrder_shouldThrowBadRequestException_whenCartDoesNotExist() {
        when(currentUserProvider.getCurrentUser()).thenReturn(customer);
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.empty());

        assertThrows(BadRequestException.class, () -> orderService.placeOrder());

        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void placeOrder_shouldThrowBadRequestException_whenCartIsEmpty() {
        when(currentUserProvider.getCurrentUser()).thenReturn(customer);
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCartId(1L)).thenReturn(List.of());

        assertThrows(BadRequestException.class, () -> orderService.placeOrder());

        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void placeOrder_shouldReduceStockAndClearCart_whenSuccessful() {
        when(currentUserProvider.getCurrentUser()).thenReturn(customer);
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCartId(1L)).thenReturn(List.of(cartItem));
        when(orderRepository.save(any(Order.class))).thenReturn(order);
        when(productRepository.saveAndFlush(any(Product.class))).thenReturn(product);
        when(orderItemRepository.findByOrderId(1000L)).thenReturn(List.of());
        when(orderMapper.toResponse(order, List.of()))
                .thenReturn(new OrderResponse(1000L, OrderStatus.PLACED, new BigDecimal("100000"), null, List.of()));

        orderService.placeOrder();

        // Stock reduced by the quantity in the cart item (5 - 2 = 3)
        assertThat(product.getStockQuantity()).isEqualTo(3);
        assertThat(meterRegistry.counter("shopsphere.orders.placed").count()).isEqualTo(1.0);
        verify(cartItemRepository).deleteAll(List.of(cartItem));
        verify(orderItemRepository).save(any(OrderItem.class));
        ArgumentCaptor<OrderPlacedEvent> eventCaptor = ArgumentCaptor.forClass(OrderPlacedEvent.class);
        verify(rabbitTemplate).convertAndSend(
                eq(RabbitMQConfig.ORDER_EXCHANGE),
                eq(RabbitMQConfig.ORDER_PLACED_ROUTING_KEY),
                eventCaptor.capture());

        OrderPlacedEvent event = eventCaptor.getValue();
        assertThat(event.orderId()).isEqualTo(1000L);
        assertThat(event.customerEmail()).isEqualTo("ravi@test.com");
        assertThat(event.totalAmount()).isEqualByComparingTo("100000");
    }

    @Test
    void placeOrder_shouldThrowBadRequestException_whenNotEnoughStock() {
        product.setStockQuantity(1);
        cartItem.setQuantity(5);

        when(currentUserProvider.getCurrentUser()).thenReturn(customer);
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCartId(1L)).thenReturn(List.of(cartItem));
        when(orderRepository.save(any(Order.class))).thenReturn(order);

        assertThrows(BadRequestException.class, () -> orderService.placeOrder());

        verify(cartItemRepository, never()).deleteAll(any());
        verify(orderItemRepository, never()).save(any(OrderItem.class));
        verifyNoInteractions(rabbitTemplate);
    }

    @Test
    void placeOrder_shouldThrowFriendlyBadRequestException_whenOptimisticLockFails() {
        when(currentUserProvider.getCurrentUser()).thenReturn(customer);
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCartId(1L)).thenReturn(List.of(cartItem));
        when(orderRepository.save(any(Order.class))).thenReturn(order);
        when(productRepository.saveAndFlush(any(Product.class)))
                .thenThrow(new OptimisticLockingFailureException("stale version"));

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> orderService.placeOrder());

        assertThat(exception.getMessage()).contains("just updated by someone else");
        verify(cartItemRepository, never()).deleteAll(any());
        verifyNoInteractions(rabbitTemplate);
    }

    @Test
    void getMyOrders_shouldReturnOnlyCurrentUsersOrders() {
        when(currentUserProvider.getCurrentUser()).thenReturn(customer);
        when(orderRepository.findByUserId(1L)).thenReturn(List.of(order));
        when(orderItemRepository.findByOrderId(1000L)).thenReturn(List.of());
        when(orderMapper.toResponse(order, List.of()))
                .thenReturn(new OrderResponse(1000L, OrderStatus.PLACED, new BigDecimal("100000"), null, List.of()));

        List<OrderResponse> result = orderService.getMyOrders();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).id()).isEqualTo(1000L);
    }

    @Test
    void getById_shouldThrowUnauthorizedActionException_whenViewedByDifferentCustomer() {
        User otherCustomer = new User();
        otherCustomer.setId(99L);
        otherCustomer.setRole(Role.CUSTOMER);

        when(orderRepository.findById(1000L)).thenReturn(Optional.of(order));
        when(currentUserProvider.getCurrentUser()).thenReturn(otherCustomer);

        assertThrows(UnauthorizedActionException.class, () -> orderService.getById(1000L));
    }

    @Test
    void getById_shouldSucceed_whenViewedByAdmin() {
        when(orderRepository.findById(1000L)).thenReturn(Optional.of(order));
        when(currentUserProvider.getCurrentUser()).thenReturn(admin);
        when(orderItemRepository.findByOrderId(1000L)).thenReturn(List.of());
        when(orderMapper.toResponse(order, List.of()))
                .thenReturn(new OrderResponse(1000L, OrderStatus.PLACED, new BigDecimal("100000"), null, List.of()));

        OrderResponse result = orderService.getById(1000L);

        assertThat(result.id()).isEqualTo(1000L);
    }

    @Test
    void getById_shouldThrowResourceNotFoundException_whenOrderDoesNotExist() {
        when(orderRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> orderService.getById(999L));
    }

    @Test
    void cancelOrder_shouldRestoreStockAndSetStatusCancelled_whenOrderIsPlaced() {
        OrderItem orderItem = new OrderItem();
        orderItem.setId(2000L);
        orderItem.setOrder(order);
        orderItem.setProduct(product);
        orderItem.setQuantity(2);
        orderItem.setPriceAtPurchase(new BigDecimal("50000"));

        when(orderRepository.findById(1000L)).thenReturn(Optional.of(order));
        when(currentUserProvider.getCurrentUser()).thenReturn(customer);
        when(orderItemRepository.findByOrderId(1000L)).thenReturn(List.of(orderItem));

        int stockBefore = product.getStockQuantity();

        orderService.cancelOrder(1000L);

        assertThat(product.getStockQuantity()).isEqualTo(stockBefore + 2);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        verify(productRepository).save(product);
    }

    @Test
    void cancelOrder_shouldThrowBadRequestException_whenOrderIsNotInPlacedStatus() {
        order.setStatus(OrderStatus.SHIPPED);

        when(orderRepository.findById(1000L)).thenReturn(Optional.of(order));
        when(currentUserProvider.getCurrentUser()).thenReturn(customer);

        assertThrows(BadRequestException.class, () -> orderService.cancelOrder(1000L));

        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    void updateStatus_shouldChangeStatus() {
        UpdateOrderStatusRequest request = new UpdateOrderStatusRequest(OrderStatus.SHIPPED);

        when(orderRepository.findById(1000L)).thenReturn(Optional.of(order));
        when(orderItemRepository.findByOrderId(1000L)).thenReturn(List.of());
        when(orderMapper.toResponse(order, List.of()))
                .thenReturn(new OrderResponse(1000L, OrderStatus.SHIPPED, new BigDecimal("100000"), null, List.of()));

        OrderResponse result = orderService.updateStatus(1000L, request);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.SHIPPED);
        assertThat(result.status()).isEqualTo(OrderStatus.SHIPPED);
    }
}