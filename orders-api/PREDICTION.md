# Prediction — DELETE /orders/{id}

> Written **before** implementing the DELETE operation, as required by the workshop (Final Challenge).
> Expected behavior: `DELETE /orders/ORD-42` → `204 No Content` · `DELETE /orders/ORD-999` → `404 Not Found`.

## 1. Which Java classes should change?

- `OrderRepository`: add `boolean deleteById(String id)`.
- `InMemoryOrderRepository`: implement it by removing the order from the map and returning whether it existed.
- `OrderService`: add `void deleteOrder(String id)`. It validates the id and throws `OrderNotFoundException` if nothing was deleted.
- `OrdersProxyHandler`: add a `DELETE` case that extracts `{id}` and calls `orderService.deleteOrder(id)`.
- `Order` and `CreateOrderRequest` should **not** change.

## 2. Do you need a new Lambda function?

- **Architecture B (proxy):** no. The same `OrdersProxyHandler` already routes by HTTP method and path, so it only needs a new case.
- **Architecture A (non-proxy):** yes, a new POJO handler (e.g. `DeleteOrderPojoHandler`) would be needed, because each operation is wired separately.

## 3. Does API Gateway configuration need to change?

Yes. `template.yaml` declares each route explicitly, so a new event `DELETE /orders/{id}` pointing to the proxy function must be added. With a greedy `{proxy+}` route no change would be needed. In Architecture A it would also require a new method, a request mapping template and response mappings for 204 and 404.

## 4. Where should the deletion logic reside?

In the application logic: `OrderService` decides and `OrderRepository` removes the data. API Gateway and the HTTP adapter should not touch the stored data.

## 5. Where should the decision to return 204 or 404 reside?

Split in two:
- `OrderService` decides **whether the order existed** (it throws `OrderNotFoundException` if it did not). It knows nothing about HTTP.
- `OrdersProxyHandler` decides **the HTTP status**: success → `204 No Content` with empty body; `OrderNotFoundException` → `404` with the standard error JSON (the existing catch already does this).

## 6. Which tests should be added?

- `OrderServiceTest`: deleting an existing order removes it (a later `getOrder` throws `OrderNotFoundException`); deleting a non-existent order throws `OrderNotFoundException`.
- `OrdersProxyHandlerTest`: `DELETE /orders/ORD-001` → 204 with empty body; `DELETE /orders/ORD-999` → 404 with `ORDER_NOT_FOUND`.
