package nz.fox.craig.product.fixture;

import java.util.List;
import java.util.UUID;
import nz.fox.craig.product.builder.ProductBuilder;
import nz.fox.craig.product.dto.ProductResponse;
import nz.fox.craig.product.model.Product;

public final class ProductResponseFixtures {

    private ProductResponseFixtures() {}

    public static ProductResponse aProductResponse() {
        Product product = ProductBuilder.aProduct().withId(UUID.randomUUID()).build();

        return ProductResponse.from(product);
    }

    public static List<ProductResponse> catalogue() {
        return ProductFixtures.catalogue().stream()
                .filter(Product::isActive)
                .map(ProductResponse::from)
                .toList();
    }
}
