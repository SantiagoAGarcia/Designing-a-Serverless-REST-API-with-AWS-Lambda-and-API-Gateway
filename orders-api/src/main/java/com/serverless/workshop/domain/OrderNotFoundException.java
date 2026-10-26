package com.serverless.workshop.domain;

/**
 * Domain exception thrown when an order with the requested ID is not found.
 */
public class OrderNotFoundException extends RuntimeException {
    private final String orderId;

    public OrderNotFoundException(String orderId) {
        super("Order " + orderId + " does not exist");
        this.orderId = orderId;
    }

    public String getOrderId() {
        return orderId;
    }
}
