package com.codenza.shopsphere.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.codenza.shopsphere.dto.OrderResponse;
import com.codenza.shopsphere.dto.UpdateOrderStatusRequest;
import com.codenza.shopsphere.entity.Cart;
import com.codenza.shopsphere.entity.CartItem;
import com.codenza.shopsphere.entity.Order;
import com.codenza.shopsphere.entity.OrderItem;
import com.codenza.shopsphere.entity.Product;
import com.codenza.shopsphere.entity.User;
import com.codenza.shopsphere.enums.OrderStatus;
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

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final OrderMapper orderMapper;
    private final CurrentUserProvider currentUserProvider;
    
    @Value("${app.order.unpaid-timeout-minutes}")
    private int unpaidTimeoutMinutes;

    @CacheEvict(value = "products", allEntries = true)
    @Transactional
    public OrderResponse placeOrder() {
        User customer = currentUserProvider.getCurrentUser();

        Cart cart = cartRepository.findByUserId(customer.getId())
                .orElseThrow(() -> new BadRequestException("Your cart is empty"));

        List<CartItem> cartItems = cartItemRepository.findByCartId(cart.getId());
        if (cartItems.isEmpty()) {
            throw new BadRequestException("Your cart is empty");
        }

        Order order = new Order();
        order.setUser(customer);
        order.setStatus(OrderStatus.PLACED);
        order.setTotalAmount(BigDecimal.ZERO);
        Order savedOrder = orderRepository.save(order);

        BigDecimal total = BigDecimal.ZERO;

        for (CartItem cartItem : cartItems) {
            Product product = cartItem.getProduct();

            try {
                reduceStock(product, cartItem.getQuantity());
            } catch (OptimisticLockingFailureException ex) {
                throw new BadRequestException(
                        "Sorry, \"" + product.getName()
                                + "\" was just updated by someone else. Please review your cart and try again.");
            }

            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(savedOrder);
            orderItem.setProduct(product);
            orderItem.setQuantity(cartItem.getQuantity());
            orderItem.setPriceAtPurchase(product.getPrice());
            orderItemRepository.save(orderItem);

            total = total.add(product.getPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity())));
        }

        savedOrder.setTotalAmount(total);

        cartItemRepository.deleteAll(cartItems);

        List<OrderItem> savedItems = orderItemRepository.findByOrderId(savedOrder.getId());
        return orderMapper.toResponse(savedOrder, savedItems);
    }

    private void reduceStock(Product product, int quantity) {
        if (product.getStockQuantity() < quantity) {
            throw new BadRequestException(
                    "Only " + product.getStockQuantity() + " unit(s) of \"" + product.getName()
                            + "\" available in stock");
        }
        product.setStockQuantity(product.getStockQuantity() - quantity);
        productRepository.saveAndFlush(product);
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getMyOrders() {
        User customer = currentUserProvider.getCurrentUser();
        List<Order> orders = orderRepository.findByUserId(customer.getId());
        return orders.stream()
                .map(order -> orderMapper.toResponse(order, orderItemRepository.findByOrderId(order.getId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public OrderResponse getById(Long id) {
        Order order = findOrder(id);
        assertViewableByCurrentUser(order);
        return orderMapper.toResponse(order, orderItemRepository.findByOrderId(order.getId()));
    }

    @CacheEvict(value = "products", allEntries = true)
    @Transactional
    public void cancelOrder(Long id) {
        Order order = findOrder(id);
        assertViewableByCurrentUser(order);

        if (order.getStatus() != OrderStatus.PLACED) {
            throw new BadRequestException("Only orders with status PLACED can be cancelled");
        }

        List<OrderItem> items = orderItemRepository.findByOrderId(order.getId());
        for (OrderItem item : items) {
            Product product = item.getProduct();
            product.setStockQuantity(product.getStockQuantity() + item.getQuantity());
            productRepository.save(product);
        }

        order.setStatus(OrderStatus.CANCELLED);
    }

    @Transactional
    public OrderResponse updateStatus(Long id, UpdateOrderStatusRequest request) {
        Order order = findOrder(id);
        order.setStatus(request.status());
        return orderMapper.toResponse(order, orderItemRepository.findByOrderId(order.getId()));
    }

    private Order findOrder(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + id));
    }

    private void assertViewableByCurrentUser(Order order) {
        User currentUser = currentUserProvider.getCurrentUser();
        boolean isAdmin = currentUser.getRole().name().equals("ADMIN");
        boolean isOwner = order.getUser().getId().equals(currentUser.getId());

        if (!isAdmin && !isOwner) {
            throw new UnauthorizedActionException("You do not have permission to view this order");
        }
    }
    
    @Scheduled(fixedRate = 5 * 60 * 1000)
    @CacheEvict(value = "products", allEntries = true)
    @Transactional
    public void cancelStaleUnpaidOrders() {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(unpaidTimeoutMinutes);
        List<Order> staleOrders = orderRepository.findByStatusAndCreatedAtBefore(OrderStatus.PLACED, cutoff);

        if (staleOrders.isEmpty()) {
            return;
        }

        log.info("Found {} stale unpaid order(s) to auto-cancel", staleOrders.size());

        for (Order order : staleOrders) {
            List<OrderItem> items = orderItemRepository.findByOrderId(order.getId());
            for (OrderItem item : items) {
                Product product = item.getProduct();
                product.setStockQuantity(product.getStockQuantity() + item.getQuantity());
                productRepository.save(product);
            }
            order.setStatus(OrderStatus.CANCELLED);
            log.info("Auto-cancelled order {} (placed at {})", order.getId(), order.getCreatedAt());
        }
    }
}