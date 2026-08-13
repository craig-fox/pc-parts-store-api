package nz.fox.craig.product.fixture;

import java.util.List;
import nz.fox.craig.product.builder.ProductResponseBuilder;
import nz.fox.craig.product.dto.ProductResponse;
import nz.fox.craig.product.model.Product;

public final class ProductResponseFixtures {

    private ProductResponseFixtures() {}

    public static ProductResponse gamingMouse() {
        Product product = ProductFixtures.gamingMouse();

        return ProductResponseBuilder.aProductResponse()
                .withId(product.getId())
                .withSku(product.getSku())
                .withName(product.getName())
                .withBrand(product.getBrand())
                .withCategory(product.getCategory())
                .withPrice(product.getPrice())
                .withStockQuantity(product.getStockQuantity())
                .withWeightKg(product.getWeightKg())
                .build();
    }

    public static List<ProductResponse> catalogue() {
        List<Product> catalogue = ProductFixtures.catalogue();

        return catalogue.stream()
                .filter(item -> item.isActive())
                .map(
                        product ->
                                ProductResponseBuilder.aProductResponse()
                                        .withId(product.getId())
                                        .withSku(product.getSku())
                                        .withName(product.getName())
                                        .withBrand(product.getBrand())
                                        .withCategory(product.getCategory())
                                        .withPrice(product.getPrice())
                                        .withStockQuantity(product.getStockQuantity())
                                        .withWeightKg(product.getWeightKg())
                                        .build())
                .toList();
    }
}
