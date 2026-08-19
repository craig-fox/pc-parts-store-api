package nz.fox.craig.order.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.Builder;

@Builder
public record OrderResponse(
        UUID id,
        UUID customerId,
        LocalDateTime orderDate,
        String status,
        BigDecimal subtotal,
        BigDecimal shipping,
        BigDecimal total,
        List<OrderItemResponse> items) {

    public OrderResponse {
        items = List.copyOf(items);
    }

}
