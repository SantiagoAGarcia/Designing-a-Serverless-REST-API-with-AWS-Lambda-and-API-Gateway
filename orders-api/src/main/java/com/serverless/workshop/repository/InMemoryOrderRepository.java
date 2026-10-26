package com.serverless.workshop.repository;

import com.serverless.workshop.domain.Order;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Thread-safe In-Memory implementation of OrderRepository.
 * Preloads sample orders to facilitate immediate manual and automated testing.
 */
public class InMemoryOrderRepository implements OrderRepository {

    private final Map<String, Order> storage = new ConcurrentHashMap<>();

    public InMemoryOrderRepository() {
        seedInitialData();
    }

    public void seedInitialData() {
        storage.put("ORD-001", new Order("ORD-001", "CUST-42", "MacBook Air", 1));
        storage.put("ORD-042", new Order("ORD-042", "CUST-7", "Keyboard", 2));
        storage.put("ORD-100", new Order("ORD-100", "CUST-42", "USB-C Adapter", 3));
    }

    @Override
    public Optional<Order> findById(String id) {
        if (id == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(storage.get(id));
    }

    @Override
    public List<Order> findByCustomerId(String customerId) {
        if (customerId == null || customerId.isBlank()) {
            return List.of();
        }
        return storage.values().stream()
                .filter(order -> customerId.equalsIgnoreCase(order.getCustomerId()))
                .collect(Collectors.toList());
    }

    @Override
    public List<Order> findAll() {
        return new ArrayList<>(storage.values());
    }

    @Override
    public Order save(Order order) {
        if (order == null || order.getId() == null) {
            throw new IllegalArgumentException("Order and Order ID must not be null");
        }
        storage.put(order.getId(), order);
        return order;
    }

    @Override
    public boolean existsById(String id) {
        return id != null && storage.containsKey(id);
    }

    @Override
    public void clear() {
        storage.clear();
    }
}
