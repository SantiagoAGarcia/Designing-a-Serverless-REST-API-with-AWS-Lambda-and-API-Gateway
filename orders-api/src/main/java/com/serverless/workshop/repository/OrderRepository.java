package com.serverless.workshop.repository;

import com.serverless.workshop.domain.Order;

import java.util.List;
import java.util.Optional;

/**
 * Interface defining persistence operations for Orders.
 * Completely decoupled from specific storage implementations.
 */
public interface OrderRepository {
    Optional<Order> findById(String id);
    List<Order> findByCustomerId(String customerId);
    List<Order> findAll();
    Order save(Order order);
    boolean deleteById(String id);
    boolean existsById(String id);
    void clear();
}
