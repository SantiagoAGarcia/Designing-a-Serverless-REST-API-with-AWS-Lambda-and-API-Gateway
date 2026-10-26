package com.serverless.workshop.lambda.proxy;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.serverless.workshop.domain.Order;
import com.serverless.workshop.repository.InMemoryOrderRepository;
import com.serverless.workshop.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("OrdersProxyHandler HTTP Adapter Tests (Architecture B)")
class OrdersProxyHandlerTest {

    private OrdersProxyHandler handler;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        InMemoryOrderRepository repository = new InMemoryOrderRepository();
        OrderService service = new OrderService(repository);
        objectMapper = new ObjectMapper();
        handler = new OrdersProxyHandler(service, objectMapper);
    }

    @Test
    @DisplayName("POST /orders creates order and returns 201 Created with Location header")
    void testPostOrdersSuccess() throws Exception {
        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent();
        request.setHttpMethod("POST");
        request.setPath("/orders");
        request.setBody("{\"customerId\":\"CUST-42\",\"product\":\"MacBook Pro\",\"quantity\":2}");

        APIGatewayProxyResponseEvent response = handler.handleRequest(request, null);

        assertEquals(201, response.getStatusCode());
        assertNotNull(response.getHeaders().get("Location"));
        assertTrue(response.getHeaders().get("Location").startsWith("/orders/ORD-"));

        JsonNode jsonNode = objectMapper.readTree(response.getBody());
        assertTrue(jsonNode.get("id").asText().startsWith("ORD-"));
        assertEquals("CUST-42", jsonNode.get("customerId").asText());
        assertEquals("MacBook Pro", jsonNode.get("product").asText());
        assertEquals(2, jsonNode.get("quantity").asInt());
    }

    @Test
    @DisplayName("POST /orders with invalid data returns 400 Bad Request")
    void testPostOrdersInvalidData() throws Exception {
        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent();
        request.setHttpMethod("POST");
        request.setPath("/orders");
        request.setBody("{\"customerId\":\"\",\"product\":\"MacBook Pro\",\"quantity\":0}");

        APIGatewayProxyResponseEvent response = handler.handleRequest(request, null);

        assertEquals(400, response.getStatusCode());
        JsonNode jsonNode = objectMapper.readTree(response.getBody());
        assertEquals("INVALID_INPUT", jsonNode.get("error").asText());
    }

    @Test
    @DisplayName("POST /orders with malformed JSON body returns 400 Bad Request")
    void testPostOrdersMalformedJson() throws Exception {
        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent();
        request.setHttpMethod("POST");
        request.setPath("/orders");
        request.setBody("{invalid-json");

        APIGatewayProxyResponseEvent response = handler.handleRequest(request, null);

        assertEquals(400, response.getStatusCode());
        JsonNode jsonNode = objectMapper.readTree(response.getBody());
        assertEquals("BAD_REQUEST", jsonNode.get("error").asText());
    }

    @Test
    @DisplayName("GET /orders/{id} for existing order returns 200 OK")
    void testGetOrderByIdSuccess() throws Exception {
        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent();
        request.setHttpMethod("GET");
        request.setPath("/orders/ORD-001");
        request.setPathParameters(Map.of("id", "ORD-001"));

        APIGatewayProxyResponseEvent response = handler.handleRequest(request, null);

        assertEquals(200, response.getStatusCode());
        JsonNode jsonNode = objectMapper.readTree(response.getBody());
        assertEquals("ORD-001", jsonNode.get("id").asText());
        assertEquals("CUST-42", jsonNode.get("customerId").asText());
    }

    @Test
    @DisplayName("GET /orders/{id} for missing order returns 404 Not Found")
    void testGetOrderByIdNotFound() throws Exception {
        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent();
        request.setHttpMethod("GET");
        request.setPath("/orders/ORD-999");
        request.setPathParameters(Map.of("id", "ORD-999"));

        APIGatewayProxyResponseEvent response = handler.handleRequest(request, null);

        assertEquals(404, response.getStatusCode());
        JsonNode jsonNode = objectMapper.readTree(response.getBody());
        assertEquals("ORDER_NOT_FOUND", jsonNode.get("error").asText());
        assertEquals("Order ORD-999 does not exist", jsonNode.get("message").asText());
    }

    @Test
    @DisplayName("GET /orders?customerId=CUST-42 returns 200 OK with list of orders")
    void testGetOrdersByCustomer() throws Exception {
        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent();
        request.setHttpMethod("GET");
        request.setPath("/orders");
        request.setQueryStringParameters(Map.of("customerId", "CUST-42"));

        APIGatewayProxyResponseEvent response = handler.handleRequest(request, null);

        assertEquals(200, response.getStatusCode());
        JsonNode jsonNode = objectMapper.readTree(response.getBody());
        assertTrue(jsonNode.isArray());
        assertEquals(2, jsonNode.size());
    }

    @Test
    @DisplayName("GET /orders without customerId query parameter returns 400 Bad Request")
    void testGetOrdersMissingQueryParam() throws Exception {
        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent();
        request.setHttpMethod("GET");
        request.setPath("/orders");

        APIGatewayProxyResponseEvent response = handler.handleRequest(request, null);

        assertEquals(400, response.getStatusCode());
        JsonNode jsonNode = objectMapper.readTree(response.getBody());
        assertEquals("INVALID_INPUT", jsonNode.get("error").asText());
    }

    @Test
    @DisplayName("DELETE /orders/{id} for existing order returns 204 No Content")
    void testDeleteOrderSuccess() {
        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent();
        request.setHttpMethod("DELETE");
        request.setPath("/orders/ORD-001");
        request.setPathParameters(Map.of("id", "ORD-001"));

        APIGatewayProxyResponseEvent response = handler.handleRequest(request, null);

        assertEquals(204, response.getStatusCode());
        assertNull(response.getBody());
    }

    @Test
    @DisplayName("DELETE /orders/{id} for missing order returns 404 Not Found")
    void testDeleteOrderNotFound() throws Exception {
        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent();
        request.setHttpMethod("DELETE");
        request.setPath("/orders/ORD-999");
        request.setPathParameters(Map.of("id", "ORD-999"));

        APIGatewayProxyResponseEvent response = handler.handleRequest(request, null);

        assertEquals(404, response.getStatusCode());
        JsonNode jsonNode = objectMapper.readTree(response.getBody());
        assertEquals("ORDER_NOT_FOUND", jsonNode.get("error").asText());
    }

    @Test
    @DisplayName("POST /orders ignores unknown JSON fields and returns 201 Created")
    void testPostOrdersIgnoresUnknownFields() throws Exception {
        APIGatewayProxyResponseEvent response = handler.handleRequest(
                request("POST", "/orders", "{\"customerId\":\"CUST-1\",\"product\":\"Mouse\",\"quantity\":1,\"extra\":true}"), null);

        assertEquals(201, response.getStatusCode());
    }

    @Test
    @DisplayName("POST /orders with a wrong field type returns 400 without parser internals")
    void testPostOrdersInvalidFieldType() throws Exception {
        APIGatewayProxyResponseEvent response = handler.handleRequest(
                request("POST", "/orders", "{\"customerId\":\"CUST-1\",\"product\":\"Mouse\",\"quantity\":\"abc\"}"), null);

        assertEquals(400, response.getStatusCode());
        JsonNode jsonNode = objectMapper.readTree(response.getBody());
        assertEquals("BAD_REQUEST", jsonNode.get("error").asText());
        assertEquals("Invalid value for field 'quantity'", jsonNode.get("message").asText());
    }

    @Test
    @DisplayName("DELETE /orders/{id} returns 204 without Content-Type header")
    void testDeleteOrderHasNoContentType() {
        APIGatewayProxyResponseEvent response = handler.handleRequest(request("DELETE", "/orders/ORD-042", null), null);

        assertEquals(204, response.getStatusCode());
        assertNull(response.getHeaders().get("Content-Type"));
    }

    @Test
    @DisplayName("Unsupported method on an existing resource returns 405 with Allow header")
    void testMethodNotAllowed() throws Exception {
        APIGatewayProxyResponseEvent itemResponse = handler.handleRequest(request("POST", "/orders/ORD-001", null), null);
        assertEquals(405, itemResponse.getStatusCode());
        assertEquals("GET, DELETE", itemResponse.getHeaders().get("Allow"));
        assertEquals("METHOD_NOT_ALLOWED", objectMapper.readTree(itemResponse.getBody()).get("error").asText());

        APIGatewayProxyResponseEvent collectionResponse = handler.handleRequest(request("PUT", "/orders", null), null);
        assertEquals(405, collectionResponse.getStatusCode());
        assertEquals("GET, POST", collectionResponse.getHeaders().get("Allow"));
    }

    @Test
    @DisplayName("Unknown path returns 404 ROUTE_NOT_FOUND")
    void testUnknownRoute() throws Exception {
        APIGatewayProxyResponseEvent response = handler.handleRequest(request("GET", "/customers", null), null);

        assertEquals(404, response.getStatusCode());
        assertEquals("ROUTE_NOT_FOUND", objectMapper.readTree(response.getBody()).get("error").asText());
    }

    @Test
    @DisplayName("Unexpected internal error returns 500 without leaking details")
    void testUnexpectedErrorReturns500() throws Exception {
        OrderService failingService = new OrderService(new InMemoryOrderRepository()) {
            @Override
            public Order getOrder(String id) {
                throw new IllegalStateException("simulated failure");
            }
        };
        OrdersProxyHandler failingHandler = new OrdersProxyHandler(failingService, objectMapper);

        APIGatewayProxyResponseEvent response = failingHandler.handleRequest(request("GET", "/orders/ORD-001", null), null);

        assertEquals(500, response.getStatusCode());
        JsonNode jsonNode = objectMapper.readTree(response.getBody());
        assertEquals("INTERNAL_SERVER_ERROR", jsonNode.get("error").asText());
        assertEquals("An unexpected internal error occurred", jsonNode.get("message").asText());
    }

    private static APIGatewayProxyRequestEvent request(String method, String path, String body) {
        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent();
        request.setHttpMethod(method);
        request.setPath(path);
        request.setBody(body);
        return request;
    }
}
