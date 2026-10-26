package com.serverless.workshop.lambda.pojo;

/**
 * POJO representing mapped request parameters from API Gateway VTL mapping template.
 * Can hold an ID (for GET /orders/{id}) or a customerId (for GET /orders?customerId=...).
 */
public class GetOrderRequest {
    private String id;
    private String customerId;

    public GetOrderRequest() {
    }

    public GetOrderRequest(String id, String customerId) {
        this.id = id;
        this.customerId = customerId;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    @Override
    public String toString() {
        return "GetOrderRequest{" +
                "id='" + id + '\'' +
                ", customerId='" + customerId + '\'' +
                '}';
    }
}
