package nz.fox.craig.product.mapper;

import java.util.List;
import nz.fox.craig.product.dto.ProductResponse;
import nz.fox.craig.product.model.Product;
import org.springframework.stereotype.Component;

@Component
public class ProductMapper {

    public ProductResponse toResponse(Product product) {
        return ProductResponse.builder()
                .id(product.getId())
                .sku(product.getSku())
                .name(product.getName())
                .description(product.getDescription())
                .brand(product.getBrand())
                .category(product.getCategory())
                .price(product.getPrice())
                .stockQuantity(product.getStockQuantity())
                .weightKg(product.getWeightKg())
                .imageUrl(product.getImageUrl())
                .build();
    }

    public List<ProductResponse> toResponseList(List<Product> products) {
        return products.stream().map(this::toResponse).toList();
    }
}
