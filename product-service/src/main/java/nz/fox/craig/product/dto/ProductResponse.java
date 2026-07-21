package nz.fox.craig.product.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record ProductResponse(
        UUID id,
        String sku,
        String name,
        String description,
        String brand,
        String category,
        BigDecimal price,
        Integer stockQuantity,
        BigDecimal weightKg,
        String imageUrl
) {
}
