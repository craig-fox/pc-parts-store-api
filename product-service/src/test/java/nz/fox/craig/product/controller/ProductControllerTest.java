package nz.fox.craig.product.controller;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.UUID;
import nz.fox.craig.product.dto.ProductResponse;
import nz.fox.craig.product.exception.ProductNotFoundException;
import nz.fox.craig.product.fixture.ProductResponseFixtures;
import nz.fox.craig.product.service.ProductService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ProductController.class)
@AutoConfigureMockMvc(addFilters = false)
public class ProductControllerTest {
    @Autowired private MockMvc mockMvc;

    @MockitoBean private ProductService productService;

    @Test
    void shouldReturnAllProducts() throws Exception {
        List<ProductResponse> catalogue = ProductResponseFixtures.catalogue();

        when(productService.getAllProducts()).thenReturn(catalogue);

        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(catalogue.size()))
                .andExpect(jsonPath("$[0].sku").value(catalogue.getFirst().sku()))
                .andExpect(jsonPath("$[0].name").value(catalogue.getFirst().name()))
                .andExpect(jsonPath("$[0].brand").value(catalogue.getFirst().brand()))
                .andExpect(jsonPath("$[1].sku").value(catalogue.get(1).sku()))
                .andExpect(jsonPath("$[1].name").value(catalogue.get(1).name()))
                .andExpect(jsonPath("$[2].sku").value(catalogue.get(2).sku()))
                .andExpect(jsonPath("$[2].name").value(catalogue.get(2).name()));

        verify(productService).getAllProducts();
    }

    @Test
    void shouldReturnProductById() throws Exception {
        ProductResponse response = ProductResponseFixtures.gamingMouse();

        when(productService.getProductById(response.id())).thenReturn(response);

        mockMvc.perform(get("/api/products/{id}", response.id()))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(response.id().toString()))
                .andExpect(jsonPath("$.sku").value(response.sku()))
                .andExpect(jsonPath("$.name").value(response.name()))
                .andExpect(jsonPath("$.brand").value(response.brand()))
                .andExpect(jsonPath("$.category").value(response.category()))
                .andExpect(jsonPath("$.price").value(response.price()))
                .andExpect(jsonPath("$.stockQuantity").value(response.stockQuantity()))
                .andExpect(jsonPath("$.weightKg").value(response.weightKg()));

        verify(productService).getProductById(response.id());
    }

    @Test
    void shouldReturnNotFoundWhenProductDoesNotExist() throws Exception {
        UUID id = UUID.randomUUID();

        when(productService.getProductById(id)).thenThrow(new ProductNotFoundException(id));

        mockMvc.perform(get("/api/products/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(
                        jsonPath("$.message")
                                .value("Product with id '%s' was not found.".formatted(id)))
                .andExpect(jsonPath("$.path").value("/api/products/" + id));

        verify(productService).getProductById(id);
    }

    @Test
    void shouldReturnBadRequestWhenProductIdIsInvalid() throws Exception {
        mockMvc.perform(get("/api/products/not-a-uuid")).andExpect(status().isBadRequest());

        verifyNoInteractions(productService);
    }

    @Test
    void shouldReturnEmptyListWhenNoProductsExist() throws Exception {
        when(productService.getAllProducts()).thenReturn(List.of());

        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(0));

        verify(productService).getAllProducts();
    }
}
