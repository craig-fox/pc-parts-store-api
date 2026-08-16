package nz.fox.craig.product.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.util.UUID;
import nz.fox.craig.product.repository.ProductRepository;
import nz.fox.craig.product.utility.ProductIds;
import nz.fox.craig.security.TokenService;
import nz.fox.craig.test.AbstractPostgresTest;
import nz.fox.craig.test.JwtTestFactory;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class ProductApiIntegrationTest extends AbstractPostgresTest {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Autowired private TokenService tokenService;

    @Autowired private MockMvc mockMvc;

    @Test
    void shouldReturnAllActiveProducts() throws Exception {
        UUID customerId = UUID.randomUUID();

        String token =
                JwtTestFactory.createToken(
                        customerId, "test@example.com", jwtSecret, Duration.ofHours(1));


        mockMvc.perform(get("/api/products").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
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
        UUID customerId = UUID.randomUUID();

        String token =
                JwtTestFactory.createToken(
                        customerId, "test@example.com", jwtSecret, Duration.ofHours(1));

        assertTrue(tokenService.isTokenValid(token));
        assertEquals(customerId, tokenService.extractCustomerId(token));
        assertEquals("test@example.com", tokenService.extractEmail(token));

        mockMvc.perform(
                        get("/api/products/{id}", productId)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.sku").value("MB-ASUS-X870E"))
                .andExpect(jsonPath("$.name").value("ASUS ROG Strix X870E-E Gaming WiFi"))
                .andExpect(jsonPath("$.brand").value("ASUS"))
                .andExpect(jsonPath("$.category").value("Motherboard"));
    }
}
