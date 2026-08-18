package nz.fox.craig.order.fixture;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import nz.fox.craig.order.model.Order;
import nz.fox.craig.order.model.OrderStatus;
import nz.fox.craig.order.model.ShippingAddress;

public final class OrderFixtures {

    private OrderFixtures() {}

    public static Order anOrder() {
        return Order.builder()
                .customerId(UUID.randomUUID())
                .orderDate(LocalDateTime.now())
                .status(OrderStatus.PLACED)
                .subtotal(new BigDecimal("1000.00"))
                .shipping(new BigDecimal("25.00"))
                .total(new BigDecimal("1025.00"))
                .shippingAddress(new ShippingAddress("1 Main St", "Auckland", "1010", "NZ"))
                .build();
    }
}
