package nz.fox.craig.order.dto.client;

import java.math.BigDecimal;
import java.util.UUID;

import lombok.Builder;

@Builder
public record ProductSnapshot(
        UUID id,
        String name,
        BigDecimal price,
        BigDecimal weightKg,
        boolean active
) {
}