package nz.fox.craig.product.dto;

import java.math.BigDecimal;
import java.util.UUID;
import nz.fox.craig.product.model.Product;

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
        String imageUrl) {

    public static ProductResponse from(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getSku(),
                product.getName(),
                product.getDescription(),
                product.getBrand(),
                product.getCategory(),
                product.getPrice(),
                product.getStockQuantity(),
                product.getWeightKg(),
                product.getImageUrl());
    }
}
