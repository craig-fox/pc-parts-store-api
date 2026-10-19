package nz.fox.craig.product.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import nz.fox.craig.product.dto.ProductResponse;
import nz.fox.craig.product.exception.ProductNotFoundException;
import nz.fox.craig.product.fixture.ProductFixtures;
import nz.fox.craig.product.model.Product;
import nz.fox.craig.product.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock private ProductRepository productRepository;

    @InjectMocks private ProductService productService;

    /** Happy Path */
    @Test
    void shouldReturnAllActiveProducts() {
        List<Product> products = ProductFixtures.catalogue();

        when(productRepository.findByActiveTrue()).thenReturn(products);

        List<ProductResponse> result = productService.getAllProducts();

        assertThat(result)
                .containsExactlyElementsOf(products.stream().map(ProductResponse::from).toList());

        verify(productRepository).findByActiveTrue();
        verifyNoMoreInteractions(productRepository);
    }

    @Test
    void shouldReturnProductById() {
        Product product = ProductFixtures.aProduct();

        when(productRepository.findById(product.getId())).thenReturn(Optional.of(product));

        ProductResponse result = productService.getProductById(product.getId());

        assertThat(result).isEqualTo(ProductResponse.from(product));

        verify(productRepository).findById(product.getId());
        verifyNoMoreInteractions(productRepository);
    }

    /** Sad Path */
    @Test
    void shouldThrowWhenProductNotFound() {
        UUID id = UUID.randomUUID();

        when(productRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.getProductById(id))
                .isInstanceOf(ProductNotFoundException.class);

        verify(productRepository).findById(id);
        verifyNoMoreInteractions(productRepository);
    }

    @Test
    void shouldThrowWhenProductIsInactive() {
        Product inactiveProduct = ProductFixtures.inactiveProduct();

        when(productRepository.findById(inactiveProduct.getId()))
                .thenReturn(Optional.of(inactiveProduct));

        assertThatThrownBy(() -> productService.getProductById(inactiveProduct.getId()))
                .isInstanceOf(ProductNotFoundException.class);

        verify(productRepository).findById(inactiveProduct.getId());
        verifyNoMoreInteractions(productRepository);
    }
}
