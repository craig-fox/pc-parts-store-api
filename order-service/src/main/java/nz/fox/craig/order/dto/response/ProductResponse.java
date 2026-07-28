package nz.fox.craig.order.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

import lombok.Builder;

@Builder
public record ProductResponse(
        UUID id,
        String name,
        BigDecimal price,
        BigDecimal weightKg,
        boolean active
) {
}