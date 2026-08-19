package nz.fox.craig.order.fixture;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import nz.fox.craig.order.dto.response.OrderItemResponse;
import nz.fox.craig.order.dto.response.OrderResponse;

public final class OrderResponseFixtures {

    private OrderResponseFixtures() {}

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
                .productId(UUID.randomUUID())
                .productName("Test Product")
                .quantity(1)
                .unitPrice(new BigDecimal("1000.00"))
                .lineTotal(new BigDecimal("1000.00"))
                .build();
    }
}
