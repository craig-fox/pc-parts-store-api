package nz.fox.craig.order.fixture;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import nz.fox.craig.api.ShippingMethod;
import nz.fox.craig.order.dto.request.OrderItemRequest;
import nz.fox.craig.order.dto.request.OrderRequest;
import nz.fox.craig.order.dto.request.ShippingAddressRequest;
import nz.fox.craig.order.dto.response.OrderItemResponse;
import nz.fox.craig.order.dto.response.OrderResponse;
import nz.fox.craig.order.model.Order;
import nz.fox.craig.order.model.OrderItem;
import nz.fox.craig.order.model.OrderStatus;
import nz.fox.craig.order.model.ShippingAddress;

public final class OrderFixture {

    public static final UUID FIXTURE_PRODUCT_ID =
            UUID.fromString("1b0d0fa6-52e1-4acd-8286-892bc29f8b3a");

    private OrderFixture() {}

    public static Order anOrder() {
        return Order.builder()
                .customerId(UUID.randomUUID())
                .idempotencyKey(UUID.randomUUID().toString())
                .idempotencyRequestHash("test-request-hash")
                .orderDate(LocalDateTime.now())
                .status(OrderStatus.PLACED)
                .subtotal(new BigDecimal("1000.00"))
                .shipping(new BigDecimal("25.00"))
                .total(new BigDecimal("1025.00"))
                .shippingAddress(new ShippingAddress("1 Main St", "Auckland", "1010", "NZ"))
                .items(new ArrayList<>(List.of(anOrderItem())))
                .build();
    }

    public static OrderRequest anOrderRequest() {
        return anOrderRequest(ShippingMethod.STANDARD, orderItems());
    }

    public static OrderRequest anOrderRequest(ShippingMethod shippingMethod) {
        return anOrderRequest(shippingMethod, orderItems());
    }

    public static OrderRequest anOrderRequest(ShippingMethod shippingMethod, List<OrderItemRequest> items) {
        return OrderRequest.builder()
                .items(items)
                .shippingAddress(shippingAddress())
                .shippingMethod(shippingMethod)
                .build();
    }

    public static List<OrderItemRequest> orderItems(int quantity) {
        return List.of(
                OrderItemRequest.builder()
                        .productId(FIXTURE_PRODUCT_ID)
                        .quantity(quantity)
                        .build());
    }

    public static List<OrderItemRequest> orderItems() {
        return orderItems(1);
    }

    public static ShippingAddressRequest shippingAddress() {
        return ShippingAddressRequest.builder()
                .addressLine1("123 Test Street")
                .city("Auckland")
                .postcode("1010")
                .country("NZ")
                .build();
    }

    public static OrderResponse anOrderResponse() {
        return OrderResponse.builder()
                .id(UUID.randomUUID())
                .customerId(UUID.randomUUID())
                .orderDate(LocalDateTime.now())
                .status("PLACED")
                .subtotal(new BigDecimal("1000.00"))
                .shipping(new BigDecimal("25.00"))
                .total(new BigDecimal("1025.00"))
                .items(List.of(anOrderItemResponse()))
                .build();
    }

    public static OrderResponse anOrderResponse(String status) {
        return OrderResponse.builder()
                .id(UUID.randomUUID())
                .customerId(UUID.randomUUID())
                .orderDate(LocalDateTime.now())
                .status(status)
                .subtotal(new BigDecimal("1000.00"))
                .shipping(new BigDecimal("25.00"))
                .total(new BigDecimal("1025.00"))
                .items(List.of(anOrderItemResponse()))
                .build();
    }

    public static OrderResponse anOrderResponse(UUID id) {
        return OrderResponse.builder()
                .id(id)
                .customerId(UUID.randomUUID())
                .orderDate(LocalDateTime.now())
                .status("PLACED")
                .subtotal(new BigDecimal("1000.00"))
                .shipping(new BigDecimal("25.00"))
                .total(new BigDecimal("1025.00"))
                .items(List.of(anOrderItemResponse()))
                .build();
    }

    private static OrderItemResponse anOrderItemResponse() {
        return OrderItemResponse.builder()
                .productId(FIXTURE_PRODUCT_ID)
                .productName("Test Product")
                .quantity(1)
                .unitPrice(new BigDecimal("1000.00"))
                .lineTotal(new BigDecimal("1000.00"))
                .build();
    }


    private static OrderItem anOrderItem() {
        return OrderItem.builder()
                .productId(FIXTURE_PRODUCT_ID)
                .productName("Test Product")
                .quantity(1)
                .unitPrice(new BigDecimal("1000.00"))
                .build();
    }
}
