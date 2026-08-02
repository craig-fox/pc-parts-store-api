package nz.fox.craig.product.mapper;

import java.util.List;

import org.springframework.stereotype.Component;

import nz.fox.craig.product.dto.ProductResponse;
import nz.fox.craig.product.model.Product;

@Component
public class ProductMapper implements ProductMapperGenerated {

    @Override
    public ProductResponse toResponse(Product product) {
        if (product == null) {
            return null;
        }

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

    @Override
    public List<ProductResponse> toResponseList(List<Product> products) {
        if (products == null) {
            return null;
        }

        return products.stream()
                .map(this::toResponse)
                .toList();
    }
}
