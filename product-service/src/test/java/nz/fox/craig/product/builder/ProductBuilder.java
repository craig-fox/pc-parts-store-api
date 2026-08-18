package nz.fox.craig.product.builder;

import java.math.BigDecimal;
import java.util.UUID;
import nz.fox.craig.product.model.Product;

public class ProductBuilder {

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
    private boolean active = true;

    private ProductBuilder() {}

    public static ProductBuilder aProduct() {
        return new ProductBuilder();
    }

    public ProductBuilder withId(UUID id) {
        this.id = id;
        return this;
    }

    public ProductBuilder withSku(String sku) {
        this.sku = sku;
        return this;
    }

    public ProductBuilder withName(String name) {
        this.name = name;
        this.description = name;
        return this;
    }

    public ProductBuilder withBrand(String brand) {
        this.brand = brand;
        return this;
    }

    public ProductBuilder withCategory(String category) {
        this.category = category;
        return this;
    }

    public ProductBuilder withPrice(BigDecimal price) {
        this.price = price;
        return this;
    }

    public ProductBuilder withStockQuantity(Integer stockQuantity) {
        this.stockQuantity = stockQuantity;
        return this;
    }

    public ProductBuilder withWeightKg(BigDecimal weightKg) {
        this.weightKg = weightKg;
        return this;
    }

    public ProductBuilder inactive() {
        this.active = false;
        return this;
    }

    public Product build() {
        Product product = new Product();

        product.setId(id);
        product.setSku(sku);
        product.setName(name);
        product.setDescription(description);
        product.setBrand(brand);
        product.setCategory(category);
        product.setPrice(price);
        product.setStockQuantity(stockQuantity);
        product.setWeightKg(weightKg);
        product.setImageUrl(imageUrl);
        product.setActive(active);

        return product;
    }
}
