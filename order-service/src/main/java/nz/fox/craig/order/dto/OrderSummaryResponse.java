package nz.fox.craig.order.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import lombok.Builder;

@Builder
public record OrderSummaryResponse(
        UUID id,
        LocalDateTime orderDate,
        String status,
        BigDecimal total
) {
}
