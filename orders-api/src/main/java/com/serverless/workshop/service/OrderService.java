package com.serverless.workshop.service;

import com.serverless.workshop.domain.CreateOrderRequest;
import com.serverless.workshop.domain.InvalidOrderException;
import com.serverless.workshop.domain.Order;
import com.serverless.workshop.domain.OrderNotFoundException;
import com.serverless.workshop.repository.OrderRepository;

import java.util.List;
import java.util.UUID;

/**
 * Core business service for managing orders.
 * Completely independent of HTTP, AWS Lambda, API Gateway, and web frameworks.
 */
public class OrderService {

    private final OrderRepository orderRepository;

    public OrderService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    /**
     * Creates and validates a new order.
     *
     * @param request the order creation details
     * @return the persisted Order entity with a generated ID
     * @throws InvalidOrderException if any validation rule is violated
     */
    public Order createOrder(CreateOrderRequest request) {
        if (request == null) {
            throw new InvalidOrderException("Request body cannot be null");
        }
        if (request.getCustomerId() == null || request.getCustomerId().trim().isEmpty()) {
            throw new InvalidOrderException("customerId is required");
        }
        if (request.getProduct() == null || request.getProduct().trim().isEmpty()) {
            throw new InvalidOrderException("product is required");
        }
        if (request.getQuantity() <= 0) {
            throw new InvalidOrderException("quantity must be greater than 0");
        }

        String generatedId = "ORD-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        Order order = new Order(
                generatedId,
                request.getCustomerId().trim(),
                request.getProduct().trim(),
                request.getQuantity()
        );

        return orderRepository.save(order);
    }

    /**
     * Retrieves an order by its unique identifier.
     *
     * @param id the order ID
     * @return the matching Order
     * @throws InvalidOrderException if ID is null or blank
     * @throws OrderNotFoundException if no order matches the ID
     */
    public Order getOrder(String id) {
        if (id == null || id.trim().isEmpty()) {
            throw new InvalidOrderException("Order id must not be empty");
        }
        return orderRepository.findById(id.trim())
                .orElseThrow(() -> new OrderNotFoundException(id.trim()));
    }

    /**
     * Finds all orders belonging to a specific customer.
     *
     * @param customerId the customer identifier
     * @return list of orders belonging to the customer
     * @throws InvalidOrderException if customerId is null or blank
     */
    public List<Order> getOrdersByCustomer(String customerId) {
        if (customerId == null || customerId.trim().isEmpty()) {
            throw new InvalidOrderException("customerId parameter is required");
        }
        return orderRepository.findByCustomerId(customerId.trim());
    }

    /**
     * Deletes an order by its ID.
     *
     * @param id the order ID to delete
     * @throws InvalidOrderException if ID is null or blank
     * @throws OrderNotFoundException if no order matches the ID
     */
    public void deleteOrder(String id) {
        if (id == null || id.trim().isEmpty()) {
            throw new InvalidOrderException("Order id must not be empty");
        }
        boolean deleted = orderRepository.deleteById(id.trim());
        if (!deleted) {
            throw new OrderNotFoundException(id.trim());
        }
    }
}
