package com.serverless.workshop.lambda.pojo;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.serverless.workshop.domain.InvalidOrderException;
import com.serverless.workshop.repository.InMemoryOrderRepository;
import com.serverless.workshop.service.OrderService;

/**
 * AWS Lambda Handler for Architecture A (Non-proxy / Custom Integration).
 * Handles retrieval by ID or query by customerId.
 *
 * Handler signature: com.serverless.workshop.lambda.pojo.GetOrderPojoHandler::handleRequest
 */
public class GetOrderPojoHandler implements RequestHandler<GetOrderRequest, Object> {

    private final OrderService orderService;

    public GetOrderPojoHandler() {
        this(new OrderService(new InMemoryOrderRepository()));
    }

    public GetOrderPojoHandler(OrderService orderService) {
        this.orderService = orderService;
    }

    @Override
    public Object handleRequest(GetOrderRequest input, Context context) {
        if (input == null) {
            throw new InvalidOrderException("Request cannot be null");
        }

        if (input.getId() != null && !input.getId().isBlank()) {
            return orderService.getOrder(input.getId());
        }

        if (input.getCustomerId() != null && !input.getCustomerId().isBlank()) {
            return orderService.getOrdersByCustomer(input.getCustomerId());
        }

        throw new InvalidOrderException("Either id or customerId must be provided");
    }
}
