package com.serverless.workshop.lambda.proxy;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.serverless.workshop.domain.CreateOrderRequest;
import com.serverless.workshop.domain.ErrorResponse;
import com.serverless.workshop.domain.InvalidOrderException;
import com.serverless.workshop.domain.Order;
import com.serverless.workshop.domain.OrderNotFoundException;
import com.serverless.workshop.repository.InMemoryOrderRepository;
import com.serverless.workshop.service.OrderService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * AWS Lambda HTTP Adapter implementing Architecture B (Lambda Proxy Integration).
 * Acts as the boundary adapter translating API Gateway proxy events into domain service calls
 * and mapping domain outcomes to proper HTTP status codes, headers, and JSON responses.
 *
 * Handler signature: com.serverless.workshop.lambda.proxy.OrdersProxyHandler::handleRequest
 */
public class OrdersProxyHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private final OrderService orderService;
    private final ObjectMapper objectMapper;

    public OrdersProxyHandler() {
        this(new OrderService(new InMemoryOrderRepository()), new ObjectMapper());
    }

    public OrdersProxyHandler(OrderService orderService, ObjectMapper objectMapper) {
        this.orderService = orderService;
        this.objectMapper = objectMapper;
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent request, Context context) {
        if (request == null) {
            return buildResponse(400, new ErrorResponse("BAD_REQUEST", "Request event cannot be null"));
        }

        String httpMethod = request.getHttpMethod() != null ? request.getHttpMethod().toUpperCase() : "";
        String path = request.getPath() != null ? request.getPath() : "";

        if (context != null && context.getLogger() != null) {
            context.getLogger().log("Incoming HTTP Request: " + httpMethod + " " + path);
        }

        try {
            switch (httpMethod) {
                case "POST":
                    if (isOrdersCollectionPath(path)) {
                        return handleCreateOrder(request);
                    }
                    break;

                case "GET":
                    String orderIdFromPath = extractOrderId(request, path);
                    if (orderIdFromPath != null) {
                        return handleGetOrderById(orderIdFromPath);
                    } else if (isOrdersCollectionPath(path)) {
                        return handleGetOrdersByCustomer(request);
                    }
                    break;

                case "OPTIONS":
                    return buildResponse(204, null, Map.of(
                            "Access-Control-Allow-Methods", "GET,POST,OPTIONS",
                            "Access-Control-Allow-Headers", "Content-Type,X-Amz-Date,Authorization,X-Api-Key"
                    ));

                default:
                    return buildResponse(405, new ErrorResponse("METHOD_NOT_ALLOWED", "Method " + httpMethod + " not supported"));
            }

            return buildResponse(404, new ErrorResponse("ROUTE_NOT_FOUND", "No route found for " + httpMethod + " " + path));

        } catch (OrderNotFoundException e) {
            return buildResponse(404, new ErrorResponse("ORDER_NOT_FOUND", e.getMessage()));
        } catch (InvalidOrderException e) {
            return buildResponse(400, new ErrorResponse("INVALID_INPUT", e.getMessage()));
        } catch (JsonProcessingException e) {
            return buildResponse(400, new ErrorResponse("BAD_REQUEST", "Malformed JSON body: " + e.getMessage()));
        } catch (Exception e) {
            if (context != null && context.getLogger() != null) {
                context.getLogger().log("Unexpected internal server error: " + e.getMessage());
            }
            return buildResponse(500, new ErrorResponse("INTERNAL_SERVER_ERROR", "An unexpected internal error occurred"));
        }
    }

    private APIGatewayProxyResponseEvent handleCreateOrder(APIGatewayProxyRequestEvent request) throws JsonProcessingException {
        if (request.getBody() == null || request.getBody().trim().isEmpty()) {
            return buildResponse(400, new ErrorResponse("INVALID_INPUT", "Request body cannot be empty"));
        }

        CreateOrderRequest createRequest = objectMapper.readValue(request.getBody(), CreateOrderRequest.class);
        Order createdOrder = orderService.createOrder(createRequest);

        Map<String, String> headers = new HashMap<>();
        headers.put("Location", "/orders/" + createdOrder.getId());

        return buildResponse(201, createdOrder, headers);
    }

    private APIGatewayProxyResponseEvent handleGetOrderById(String orderId) {
        Order order = orderService.getOrder(orderId);
        return buildResponse(200, order);
    }

    private APIGatewayProxyResponseEvent handleGetOrdersByCustomer(APIGatewayProxyRequestEvent request) {
        Map<String, String> queryParams = request.getQueryStringParameters();
        String customerId = queryParams != null ? queryParams.get("customerId") : null;

        if (customerId == null || customerId.trim().isEmpty()) {
            return buildResponse(400, new ErrorResponse("INVALID_INPUT", "customerId query parameter is required"));
        }

        List<Order> orders = orderService.getOrdersByCustomer(customerId);
        return buildResponse(200, orders);
    }

    private boolean isOrdersCollectionPath(String path) {
        return "/orders".equalsIgnoreCase(path) || "/orders/".equalsIgnoreCase(path);
    }

    private String extractOrderId(APIGatewayProxyRequestEvent request, String path) {
        if (request.getPathParameters() != null && request.getPathParameters().containsKey("id")) {
            return request.getPathParameters().get("id");
        }
        // Fallback for path matching /orders/{id}
        if (path.startsWith("/orders/") && path.length() > 8) {
            String remainder = path.substring(8);
            if (!remainder.contains("/")) {
                return remainder;
            }
        }
        return null;
    }

    private APIGatewayProxyResponseEvent buildResponse(int statusCode, Object body) {
        return buildResponse(statusCode, body, Map.of());
    }

    private APIGatewayProxyResponseEvent buildResponse(int statusCode, Object body, Map<String, String> extraHeaders) {
        APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();
        response.setStatusCode(statusCode);

        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        headers.put("Access-Control-Allow-Origin", "*");
        if (extraHeaders != null) {
            headers.putAll(extraHeaders);
        }
        response.setHeaders(headers);

        if (body != null) {
            try {
                response.setBody(objectMapper.writeValueAsString(body));
            } catch (JsonProcessingException e) {
                response.setStatusCode(500);
                response.setBody("{\"error\":\"SERIALIZATION_ERROR\",\"message\":\"Failed to serialize response\"}");
            }
        } else {
            response.setBody(null);
        }

        return response;
    }
}
