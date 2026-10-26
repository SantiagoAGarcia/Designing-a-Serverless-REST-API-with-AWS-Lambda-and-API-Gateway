package com.serverless.workshop.lambda.proxy;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import com.serverless.workshop.domain.CreateOrderRequest;
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
 * Translates API Gateway proxy events into domain service calls and maps domain
 * outcomes to HTTP status codes, headers and JSON bodies.
 *
 * Routes:
 *   /orders       GET (requires ?customerId=...), POST
 *   /orders/{id}  GET, DELETE
 * A known path with an unsupported method returns 405; an unknown path returns 404.
 *
 * Handler signature: com.serverless.workshop.lambda.proxy.OrdersProxyHandler::handleRequest
 */
public class OrdersProxyHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private static final String COLLECTION_METHODS = "GET, POST";
    private static final String ITEM_METHODS = "GET, DELETE";

    private final OrderService orderService;
    private final ObjectMapper objectMapper;

    public OrdersProxyHandler() {
        this(new OrderService(new InMemoryOrderRepository()), new ObjectMapper());
    }

    public OrdersProxyHandler(OrderService orderService, ObjectMapper objectMapper) {
        this.orderService = orderService;
        // Unknown JSON fields are ignored, the same way the Lambda runtime does for POJO handlers.
        this.objectMapper = objectMapper.copy()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent request, Context context) {
        if (request == null) {
            return error(400, "BAD_REQUEST", "Request event cannot be null");
        }

        String httpMethod = request.getHttpMethod() != null ? request.getHttpMethod().toUpperCase() : "";
        String path = request.getPath() != null ? request.getPath() : "";
        logEvent(request, context);

        try {
            if (isOrdersCollectionPath(path)) {
                switch (httpMethod) {
                    case "GET":
                        return handleGetOrdersByCustomer(request);
                    case "POST":
                        return handleCreateOrder(request);
                    default:
                        return methodNotAllowed(httpMethod, path, COLLECTION_METHODS);
                }
            }

            String orderId = extractOrderId(request, path);
            if (orderId != null) {
                switch (httpMethod) {
                    case "GET":
                        return buildResponse(200, orderService.getOrder(orderId));
                    case "DELETE":
                        orderService.deleteOrder(orderId);
                        return buildResponse(204, null);
                    default:
                        return methodNotAllowed(httpMethod, path, ITEM_METHODS);
                }
            }

            return error(404, "ROUTE_NOT_FOUND", "No route found for " + httpMethod + " " + path);

        } catch (OrderNotFoundException e) {
            return error(404, "ORDER_NOT_FOUND", e.getMessage());
        } catch (InvalidOrderException e) {
            return error(400, "INVALID_INPUT", e.getMessage());
        } catch (JsonProcessingException e) {
            return error(400, "BAD_REQUEST", describeJsonError(e));
        } catch (Exception e) {
            if (context != null && context.getLogger() != null) {
                context.getLogger().log("Unexpected internal server error: " + e);
            }
            return error(500, "INTERNAL_SERVER_ERROR", "An unexpected internal error occurred");
        }
    }

    private APIGatewayProxyResponseEvent handleCreateOrder(APIGatewayProxyRequestEvent request) throws JsonProcessingException {
        if (request.getBody() == null || request.getBody().trim().isEmpty()) {
            return error(400, "INVALID_INPUT", "Request body cannot be empty");
        }

        CreateOrderRequest createRequest = objectMapper.readValue(request.getBody(), CreateOrderRequest.class);
        Order createdOrder = orderService.createOrder(createRequest);

        return buildResponse(201, createdOrder, Map.of("Location", "/orders/" + createdOrder.getId()));
    }

    private APIGatewayProxyResponseEvent handleGetOrdersByCustomer(APIGatewayProxyRequestEvent request) {
        Map<String, String> queryParams = request.getQueryStringParameters();
        String customerId = queryParams != null ? queryParams.get("customerId") : null;

        if (customerId == null || customerId.trim().isEmpty()) {
            return error(400, "INVALID_INPUT", "customerId query parameter is required");
        }

        List<Order> orders = orderService.getOrdersByCustomer(customerId);
        return buildResponse(200, orders);
    }

    /** Builds a client-friendly message without exposing Java class names or parser internals. */
    private String describeJsonError(JsonProcessingException e) {
        if (e instanceof MismatchedInputException mismatch && !mismatch.getPath().isEmpty()) {
            JsonMappingException.Reference field = mismatch.getPath().get(mismatch.getPath().size() - 1);
            if (field.getFieldName() != null) {
                return "Invalid value for field '" + field.getFieldName() + "'";
            }
        }
        return "Request body is not valid JSON";
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

    /** Logs the complete proxy event so a real example can be copied from CloudWatch Logs. */
    private void logEvent(APIGatewayProxyRequestEvent request, Context context) {
        if (context == null || context.getLogger() == null) {
            return;
        }
        try {
            context.getLogger().log("API Gateway proxy event: " + objectMapper.writeValueAsString(request));
        } catch (Exception e) {
            context.getLogger().log("Incoming HTTP Request: " + request.getHttpMethod() + " " + request.getPath());
        }
    }

    private APIGatewayProxyResponseEvent methodNotAllowed(String method, String path, String allowed) {
        return buildResponse(405,
                new ErrorResponse("METHOD_NOT_ALLOWED", "Method " + method + " is not allowed on " + path),
                Map.of("Allow", allowed));
    }

    private APIGatewayProxyResponseEvent error(int statusCode, String code, String message) {
        return buildResponse(statusCode, new ErrorResponse(code, message));
    }

    private APIGatewayProxyResponseEvent buildResponse(int statusCode, Object body) {
        return buildResponse(statusCode, body, Map.of());
    }

    private APIGatewayProxyResponseEvent buildResponse(int statusCode, Object body, Map<String, String> extraHeaders) {
        APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();
        response.setStatusCode(statusCode);

        Map<String, String> headers = new HashMap<>();
        headers.put("Access-Control-Allow-Origin", "*");
        headers.putAll(extraHeaders);

        if (body != null) {
            headers.put("Content-Type", "application/json");
            try {
                response.setBody(objectMapper.writeValueAsString(body));
            } catch (JsonProcessingException e) {
                response.setStatusCode(500);
                response.setBody("{\"error\":\"INTERNAL_SERVER_ERROR\",\"message\":\"Failed to serialize response\"}");
            }
        }
        // 204 No Content: no body and therefore no Content-Type header.
        response.setHeaders(headers);
        return response;
    }
}
