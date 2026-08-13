package nz.fox.craig.product.service;

import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import nz.fox.craig.product.dto.ProductResponse;
import nz.fox.craig.product.exception.ProductNotFoundException;
import nz.fox.craig.product.mapper.ProductMapper;
import nz.fox.craig.product.model.Product;
import nz.fox.craig.product.repository.ProductRepository;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductMapper productMapper;

    public List<ProductResponse> getAllProducts() {
        return productMapper.toResponseList(productRepository.findByActiveTrue());
    }

    public ProductResponse getProductById(UUID id) {
        Product product =
                productRepository
                        .findById(id)
                        .filter(Product::isActive)
                        .orElseThrow(() -> new ProductNotFoundException(id));

        return productMapper.toResponse(product);
    }
}
