package nz.fox.craig.product.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import nz.fox.craig.product.repository.ProductRepository;
import nz.fox.craig.product.utility.ProductIds;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class ProductApiIntegrationTest extends AbstractPostgresTest {

    @Autowired private MockMvc mockMvc;

    @Autowired private ProductRepository productRepository;

    @Test
    void shouldReturnAllActiveProducts() throws Exception {
        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(20))

                // Verify the first seeded product
                .andExpect(jsonPath("$[0].sku").value("CPU-AMD-9800X3D"))
                .andExpect(jsonPath("$[0].name").value("AMD Ryzen 7 9800X3D"))
                .andExpect(jsonPath("$[0].brand").value("AMD"))
                .andExpect(jsonPath("$[0].category").value("CPU"))

                // Verify another product further down the list
                .andExpect(jsonPath("$[5].sku").value("MB-MSI-Z890"))
                .andExpect(jsonPath("$[5].brand").value("MSI"));
    }

    @Test
    void shouldReturnProductById() throws Exception {
        String productId = ProductIds.ASUS_X870E.toString();
        mockMvc.perform(get(String.format("/api/products/%s", productId)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.sku").value("MB-ASUS-X870E"))
                .andExpect(jsonPath("$.name").value("ASUS ROG Strix X870E-E Gaming WiFi"))
                .andExpect(jsonPath("$.brand").value("ASUS"))
                .andExpect(jsonPath("$.category").value("Motherboard"));
    }

    @Test
    void listSeededProducts() {
        productRepository
                .findAll()
                .forEach(
                        product ->
                                System.out.printf("%s | %s%n", product.getId(), product.getSku()));
    }
}
