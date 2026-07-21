package nz.fox.craig.product.mapper;

import java.util.List;

import org.mapstruct.Mapper;

import nz.fox.craig.product.dto.ProductResponse;
import nz.fox.craig.product.model.Product;

@Mapper(componentModel = "spring")
public interface ProductMapper {

    ProductResponse toResponse(Product product);

    List<ProductResponse> toResponseList(List<Product> products);
}
