package nz.fox.craig.order.integration;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import com.fasterxml.jackson.databind.ObjectMapper;

import nz.fox.craig.order.dto.client.ProductSnapshot;
import nz.fox.craig.order.dto.request.OrderItemRequest;
import nz.fox.craig.order.dto.request.OrderRequest;
import nz.fox.craig.order.repository.AbstractPostgresTest;
import nz.fox.craig.order.security.JwtService;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class OrderIntegrationTest extends AbstractPostgresTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    static MockWebServer mockWebServer;

    @Value("${jwt.secret}")
    private String jwtSecret;

    private final UUID productId = UUID.fromString("1b0d0fa6-52e1-4acd-8286-892bc29f8b3a");

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        registry.add("services.customer.base-url", () -> mockWebServer.url("/").toString());
        registry.add("services.product.base-url", () -> mockWebServer.url("/").toString());
        registry.add("services.inventory.base-url", () -> mockWebServer.url("/").toString());
    }

    @BeforeAll
    static void startServer() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();
    }

    @AfterAll
    static void stopServer() throws IOException {
        mockWebServer.shutdown();
    }

    @Test
    @Timeout(10)
    void shouldCreateOrderForExistingCustomer() throws Exception {

        UUID customerId = UUID.randomUUID();

        String token = JwtTestFactory.createToken(
                customerId,
                "test@example.com",
                jwtSecret,
                Duration.ofHours(1));

        final List<OrderItemRequest> itemRequests =
                List.of(new OrderItemRequest(productId, 2));

        final OrderRequest request =
            new OrderRequest(customerId, itemRequests);

           

        // Customer validation
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .addHeader("Content-Length", "0"));

        // Inventory reservation
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .addHeader("Content-Length", "0"));

        // Product lookup
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody(objectMapper.writeValueAsString(productSnapshot()))
                .addHeader("Content-Type", "application/json"));

        mockMvc.perform(post("/api/orders")
        .header("Authorization", "Bearer " + token)
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated());    

        RecordedRequest recordedRequest = mockWebServer.takeRequest();

        assertEquals("HEAD", recordedRequest.getMethod());
        assertTrue(recordedRequest.getPath().startsWith("/api/customers/"));
    }

    private ProductSnapshot productSnapshot() {
        return ProductSnapshot.builder()
                .id(productId)
                .name("Gaming Mouse")
                .price(new BigDecimal("89.99"))
                .weightKg(new BigDecimal("0.30"))
                .active(true)
                .build();
    }

}
