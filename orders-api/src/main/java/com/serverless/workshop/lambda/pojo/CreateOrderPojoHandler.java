package com.serverless.workshop.lambda.pojo;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.serverless.workshop.domain.CreateOrderRequest;
import com.serverless.workshop.domain.Order;
import com.serverless.workshop.repository.InMemoryOrderRepository;
import com.serverless.workshop.service.OrderService;

/**
 * AWS Lambda Handler for Architecture A (POJO direct invocation / Non-proxy integration).
 * Lambda runtime deserializes input JSON into CreateOrderRequest POJO automatically,
 * and serializes the returned Order POJO into JSON.
 *
 * Handler signature: com.serverless.workshop.lambda.pojo.CreateOrderPojoHandler::handleRequest
 */
public class CreateOrderPojoHandler implements RequestHandler<CreateOrderRequest, Order> {

    private final OrderService orderService;

    public CreateOrderPojoHandler() {
        this(new OrderService(new InMemoryOrderRepository()));
    }

    public CreateOrderPojoHandler(OrderService orderService) {
        this.orderService = orderService;
    }

    @Override
    public Order handleRequest(CreateOrderRequest input, Context context) {
        if (context != null && context.getLogger() != null) {
            context.getLogger().log("Processing CreateOrder POJO request: " + input);
        }
        return orderService.createOrder(input);
    }
}
