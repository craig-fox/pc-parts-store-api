package nz.fox.craig.product.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import nz.fox.craig.product.dto.ProductResponse;
import nz.fox.craig.product.exception.ProductNotFoundException;
import nz.fox.craig.product.fixture.ProductFixtures;
import nz.fox.craig.product.fixture.ProductResponseFixtures;
import nz.fox.craig.product.mapper.ProductMapper;
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

    @Mock private ProductMapper productMapper;

    @InjectMocks private ProductService productService;

    /** Happy Path */
    @Test
    void shouldReturnAllActiveProducts() {
        List<Product> products = ProductFixtures.catalogue();
        List<ProductResponse> responses = ProductResponseFixtures.catalogue();

        when(productRepository.findByActiveTrue()).thenReturn(products);
        when(productMapper.toResponseList(products)).thenReturn(responses);

        List<ProductResponse> result = productService.getAllProducts();
        assertEquals(responses, result);

        verify(productRepository).findByActiveTrue();
        verify(productMapper).toResponseList(products);
        verifyNoMoreInteractions(productRepository, productMapper);
    }

    @Test
    void shouldReturnProductById() {
        // Arrange
        Product product = ProductFixtures.aProduct();
        ProductResponse expectedResponse = ProductResponseFixtures.aProductResponse();

        when(productRepository.findById(product.getId())).thenReturn(Optional.of(product));
        when(productMapper.toResponse(product)).thenReturn(expectedResponse);

        // Act
        ProductResponse result = productService.getProductById(product.getId());

        // Assert
        assertEquals(expectedResponse, result);

        verify(productRepository).findById(product.getId());
        verify(productMapper).toResponse(product);
        verifyNoMoreInteractions(productRepository, productMapper);
    }

    /** Sad Path */
    @Test
    void shouldThrowWhenProductNotFound() {
        UUID id = UUID.randomUUID();

        when(productRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(ProductNotFoundException.class, () -> productService.getProductById(id));

        verify(productRepository).findById(id);
        verifyNoInteractions(productMapper);
    }

    @Test
    void shouldThrowWhenProductIsInactive() {
        UUID id = UUID.randomUUID();

        Product inactiveProduct = ProductFixtures.inactiveProduct();

        when(productRepository.findById(id)).thenReturn(Optional.of(inactiveProduct));

        assertThrows(ProductNotFoundException.class, () -> productService.getProductById(id));

        verify(productRepository).findById(id);
        verifyNoInteractions(productMapper);
    }
}
