package com.codenza.shopsphere.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.codenza.shopsphere.entity.OrderItem;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    List<OrderItem> findByOrderId(Long orderId);
}