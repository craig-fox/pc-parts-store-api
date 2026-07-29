package nz.fox.craig.order.integration;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import com.fasterxml.jackson.databind.ObjectMapper;

import nz.fox.craig.order.dto.client.ProductSnapshot;
import nz.fox.craig.order.dto.request.CreateOrderItemRequest;
import nz.fox.craig.order.dto.request.CreateOrderRequest;
import nz.fox.craig.order.repository.AbstractPostgresTest;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@SpringBootTest
@AutoConfigureMockMvc
class OrderIntegrationTest extends AbstractPostgresTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    static MockWebServer mockWebServer;

    private final UUID productId = UUID.fromString("1b0d0fa6-52e1-4acd-8286-892bc29f8b3a") ;    

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        registry.add("services.customer.base-url", () -> mockWebServer.url("/").toString());
        registry.add("services.product.base-url", () -> mockWebServer.url("/").toString());
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

        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(200)
            .addHeader("Content-Length", "0"));
        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(200)
            .setBody(objectMapper.writeValueAsString(productSnapshot()))
                .addHeader("Content-Type", "application/json")); // product lookup        
           
        final List<CreateOrderItemRequest> itemRequests = List.of(new CreateOrderItemRequest(productId, 2));
        final CreateOrderRequest request = new CreateOrderRequest(UUID.randomUUID(), itemRequests);

        mockMvc.perform(post("/api/orders")
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
