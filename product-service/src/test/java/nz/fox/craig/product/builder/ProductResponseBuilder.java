package nz.fox.craig.product.builder;

import java.math.BigDecimal;
import java.util.UUID;
import nz.fox.craig.product.dto.ProductResponse;

public class ProductResponseBuilder {

    private UUID id = UUID.randomUUID();
    private String sku = "CPU-001";
    private String name = "AMD Ryzen 7 9800X3D";
    private String description = name;
    private String brand = "AMD";
    private String category = "CPU";
    private BigDecimal price = new BigDecimal("899.99");
    private Integer stockQuantity = 10;
    private BigDecimal weightKg = new BigDecimal("0.15");
    private String imageUrl = "https://example.com/images/CPU-001.jpg";

    private ProductResponseBuilder() { }

    public static ProductResponseBuilder aProductResponse() {
        return new ProductResponseBuilder();
    }

    public ProductResponseBuilder withId(UUID id) {
        this.id = id;
        return this;
    }

    public ProductResponseBuilder withSku(String sku) {
        this.sku = sku;
        this.imageUrl = "https://example.com/images/" + sku + ".jpg";
        return this;
    }

    public ProductResponseBuilder withName(String name) {
        this.name = name;
        this.description = name;
        return this;
    }

    public ProductResponseBuilder withBrand(String brand) {
        this.brand = brand;
        return this;
    }

    public ProductResponseBuilder withCategory(String category) {
        this.category = category;
        return this;
    }

    public ProductResponseBuilder withPrice(BigDecimal price) {
        this.price = price;
        return this;
    }

    public ProductResponseBuilder withStockQuantity(Integer stockQuantity) {
        this.stockQuantity = stockQuantity;
        return this;
    }

    public ProductResponseBuilder withWeightKg(BigDecimal weightKg) {
        this.weightKg = weightKg;
        return this;
    }

    public ProductResponse build() {
        return new ProductResponse(
                id,
                sku,
                name,
                description,
                brand,
                category,
                price,
                stockQuantity,
                weightKg,
                imageUrl);
    }
}
