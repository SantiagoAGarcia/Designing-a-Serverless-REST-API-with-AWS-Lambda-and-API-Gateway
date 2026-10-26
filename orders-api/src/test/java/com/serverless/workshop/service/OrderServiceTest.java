package com.serverless.workshop.service;

import com.serverless.workshop.domain.CreateOrderRequest;
import com.serverless.workshop.domain.InvalidOrderException;
import com.serverless.workshop.domain.Order;
import com.serverless.workshop.domain.OrderNotFoundException;
import com.serverless.workshop.repository.InMemoryOrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("OrderService Unit Tests (Plain Java Business Logic)")
class OrderServiceTest {

    private InMemoryOrderRepository repository;
    private OrderService service;

    @BeforeEach
    void setUp() {
        repository = new InMemoryOrderRepository();
        service = new OrderService(repository);
    }

    @Test
    @DisplayName("Creation of a valid order should return Order with generated ID")
    void testCreateValidOrder() {
        CreateOrderRequest request = new CreateOrderRequest("CUST-42", "MacBook Air", 1);
        Order created = service.createOrder(request);

        assertNotNull(created);
        assertNotNull(created.getId());
        assertTrue(created.getId().startsWith("ORD-"));
        assertEquals("CUST-42", created.getCustomerId());
        assertEquals("MacBook Air", created.getProduct());
        assertEquals(1, created.getQuantity());

        // Verify it was persisted in repository
        Order retrieved = service.getOrder(created.getId());
        assertEquals(created, retrieved);
    }

    @Test
    @DisplayName("Retrieval of an existing pre-seeded order")
    void testGetExistingOrder() {
        Order order = service.getOrder("ORD-001");
        assertNotNull(order);
        assertEquals("ORD-001", order.getId());
        assertEquals("CUST-42", order.getCustomerId());
        assertEquals("MacBook Air", order.getProduct());
        assertEquals(1, order.getQuantity());
    }

    @Test
    @DisplayName("Behavior when an order does not exist should throw OrderNotFoundException")
    void testGetNonExistentOrderThrowsException() {
        OrderNotFoundException ex = assertThrows(OrderNotFoundException.class, () -> service.getOrder("ORD-999"));
        assertEquals("Order ORD-999 does not exist", ex.getMessage());
        assertEquals("ORD-999", ex.getOrderId());
    }

    @Test
    @DisplayName("Searching orders by customer returns matching list")
    void testSearchOrdersByCustomer() {
        List<Order> orders = service.getOrdersByCustomer("CUST-42");
        assertNotNull(orders);
        assertEquals(2, orders.size());
        assertTrue(orders.stream().allMatch(o -> "CUST-42".equals(o.getCustomerId())));

        List<Order> emptyOrders = service.getOrdersByCustomer("CUST-UNKNOWN");
        assertNotNull(emptyOrders);
        assertTrue(emptyOrders.isEmpty());
    }

    @Test
    @DisplayName("Invalid input for order creation throws InvalidOrderException")
    void testInvalidOrderCreationInput() {
        // Null request
        assertThrows(InvalidOrderException.class, () -> service.createOrder(null));

        // Missing customerId
        assertThrows(InvalidOrderException.class, () ->
                service.createOrder(new CreateOrderRequest(null, "MacBook Air", 1)));
        assertThrows(InvalidOrderException.class, () ->
                service.createOrder(new CreateOrderRequest("   ", "MacBook Air", 1)));

        // Missing product
        assertThrows(InvalidOrderException.class, () ->
                service.createOrder(new CreateOrderRequest("CUST-42", null, 1)));
        assertThrows(InvalidOrderException.class, () ->
                service.createOrder(new CreateOrderRequest("CUST-42", "   ", 1)));

        // Invalid quantity <= 0
        assertThrows(InvalidOrderException.class, () ->
                service.createOrder(new CreateOrderRequest("CUST-42", "MacBook Air", 0)));
        assertThrows(InvalidOrderException.class, () ->
                service.createOrder(new CreateOrderRequest("CUST-42", "MacBook Air", -5)));
    }

    @Test
    @DisplayName("Invalid input for retrieval and search throws InvalidOrderException")
    void testInvalidInputForRetrievalAndSearch() {
        assertThrows(InvalidOrderException.class, () -> service.getOrder(null));
        assertThrows(InvalidOrderException.class, () -> service.getOrder("   "));

        assertThrows(InvalidOrderException.class, () -> service.getOrdersByCustomer(null));
        assertThrows(InvalidOrderException.class, () -> service.getOrdersByCustomer("   "));
    }

    @Test
    @DisplayName("Deleting an existing order succeeds")
    void testDeleteExistingOrder() {
        assertNotNull(service.getOrder("ORD-001"));
        service.deleteOrder("ORD-001");
        assertThrows(OrderNotFoundException.class, () -> service.getOrder("ORD-001"));
    }

    @Test
    @DisplayName("Deleting a non-existent order throws OrderNotFoundException")
    void testDeleteNonExistentOrder() {
        assertThrows(OrderNotFoundException.class, () -> service.deleteOrder("ORD-999"));
    }
}
