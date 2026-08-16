package nz.fox.craig.product.fixture;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import nz.fox.craig.product.builder.ProductBuilder;
import nz.fox.craig.product.model.Product;

public final class ProductFixtures {

    private ProductFixtures() { }

    public static Product gamingMouse() {
        return ProductBuilder.aProduct()
                .withId(UUID.fromString("11111111-1111-1111-1111-111111111111"))
                .withSku("LOG-G502X")
                .withName("Logitech G502 X Plus")
                .withBrand("Logitech")
                .withCategory("Peripherals")
                .withPrice(new BigDecimal("249.99"))
                .withStockQuantity(25)
                .withWeightKg(new BigDecimal("0.11"))
                .build();
    }

    public static Product inactiveProduct() {
        return ProductBuilder.aProduct()
                .withSku("SSD-001")
                .withName("Samsung 990 Pro 2TB NVMe SSD")
                .withBrand("Samsung")
                .withCategory("Storage")
                .withPrice(new BigDecimal("399.99"))
                .inactive()
                .build();
    }

    public static List<Product> catalogue() {
        return List.of(
                ProductBuilder.aProduct().build(),
                ProductBuilder.aProduct()
                        .withSku("GPU-001")
                        .withName("NVIDIA RTX 5080")
                        .withBrand("NVIDIA")
                        .withCategory("GPU")
                        .withPrice(new BigDecimal("2499.99"))
                        .withWeightKg(new BigDecimal("1.85"))
                        .build(),
                ProductBuilder.aProduct()
                        .withSku("RAM-001")
                        .withName("Corsair Vengeance 32GB DDR5")
                        .withBrand("Corsair")
                        .withCategory("Memory")
                        .withPrice(new BigDecimal("329.99"))
                        .build(),
                ProductBuilder.aProduct()
                        .withSku("SSD-001")
                        .withName("Samsung 990 Pro 2TB NVMe SSD")
                        .withBrand("Samsung")
                        .withCategory("Storage")
                        .withPrice(new BigDecimal("399.99"))
                        .inactive()
                        .build());
    }
}
