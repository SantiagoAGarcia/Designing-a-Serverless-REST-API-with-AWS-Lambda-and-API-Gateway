package com.serverless.workshop.lambda.pojo;

import com.serverless.workshop.domain.CreateOrderRequest;
import com.serverless.workshop.domain.InvalidOrderException;
import com.serverless.workshop.domain.Order;
import com.serverless.workshop.domain.OrderNotFoundException;
import com.serverless.workshop.repository.InMemoryOrderRepository;
import com.serverless.workshop.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("POJO Lambda Handlers Tests (Architecture A)")
class PojoHandlersTest {

    private OrderService orderService;
    private CreateOrderPojoHandler createHandler;
    private GetOrderPojoHandler getHandler;

    @BeforeEach
    void setUp() {
        InMemoryOrderRepository repository = new InMemoryOrderRepository();
        orderService = new OrderService(repository);
        createHandler = new CreateOrderPojoHandler(orderService);
        getHandler = new GetOrderPojoHandler(orderService);
    }

    @Test
    @DisplayName("CreateOrderPojoHandler successfully processes CreateOrderRequest")
    void testCreateOrderPojo() {
        CreateOrderRequest request = new CreateOrderRequest("CUST-99", "iPad Pro", 1);
        Order created = createHandler.handleRequest(request, null);

        assertNotNull(created);
        assertNotNull(created.getId());
        assertEquals("CUST-99", created.getCustomerId());
        assertEquals("iPad Pro", created.getProduct());
    }

    @Test
    @DisplayName("GetOrderPojoHandler retrieves existing order by ID")
    void testGetOrderPojoById() {
        GetOrderRequest request = new GetOrderRequest("ORD-001", null);
        Object result = getHandler.handleRequest(request, null);

        assertTrue(result instanceof Order);
        Order order = (Order) result;
        assertEquals("ORD-001", order.getId());
        assertEquals("CUST-42", order.getCustomerId());
    }

    @Test
    @DisplayName("GetOrderPojoHandler retrieves orders by customerId")
    void testGetOrderPojoByCustomer() {
        GetOrderRequest request = new GetOrderRequest(null, "CUST-42");
        Object result = getHandler.handleRequest(request, null);

        assertTrue(result instanceof List);
        List<?> list = (List<?>) result;
        assertEquals(2, list.size());
    }

    @Test
    @DisplayName("GetOrderPojoHandler throws OrderNotFoundException when order does not exist")
    void testGetOrderPojoNotFound() {
        GetOrderRequest request = new GetOrderRequest("ORD-NONEXISTENT", null);
        assertThrows(OrderNotFoundException.class, () -> getHandler.handleRequest(request, null));
    }

    @Test
    @DisplayName("GetOrderPojoHandler throws InvalidOrderException when empty request provided")
    void testGetOrderPojoEmptyRequest() {
        GetOrderRequest request = new GetOrderRequest();
        assertThrows(InvalidOrderException.class, () -> getHandler.handleRequest(request, null));
    }
}
