package com.serverless.workshop.domain;

import java.util.Objects;

/**
 * Data Transfer Object for creating a new Order.
 * Pure Java POJO without framework annotations.
 */
public class CreateOrderRequest {
    private String customerId;
    private String product;
    private int quantity;

    public CreateOrderRequest() {
    }

    public CreateOrderRequest(String customerId, String product, int quantity) {
        this.customerId = customerId;
        this.product = product;
        this.quantity = quantity;
    }

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public String getProduct() {
        return product;
    }

    public void setProduct(String product) {
        this.product = product;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CreateOrderRequest that)) return false;
        return quantity == that.quantity &&
                Objects.equals(customerId, that.customerId) &&
                Objects.equals(product, that.product);
    }

    @Override
    public int hashCode() {
        return Objects.hash(customerId, product, quantity);
    }

    @Override
    public String toString() {
        return "CreateOrderRequest{" +
                "customerId='" + customerId + '\'' +
                ", product='" + product + '\'' +
                ", quantity=" + quantity +
                '}';
    }
}
