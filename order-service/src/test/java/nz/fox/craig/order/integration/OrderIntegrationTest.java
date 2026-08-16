package nz.fox.craig.order.integration;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import nz.fox.craig.order.dto.client.ProductSnapshot;
import nz.fox.craig.order.dto.request.OrderItemRequest;
import nz.fox.craig.order.dto.request.OrderRequest;
import nz.fox.craig.order.dto.request.ShippingAddressRequest;
import nz.fox.craig.test.AbstractPostgresTest;
import nz.fox.craig.test.JwtTestFactory;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

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

@SpringBootTest
@AutoConfigureMockMvc
class OrderIntegrationTest extends AbstractPostgresTest {

    @Autowired private MockMvc mockMvc;

    @Autowired private ObjectMapper objectMapper;

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
        String token = createToken(customerId);

        OrderRequest request = createOrderRequest();

        enqueueSuccessfulOrderDependencies();

        mockMvc.perform(
                        post("/api/orders")
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.customerId").value(customerId.toString()))
                .andExpect(jsonPath("$.orderDate").exists())
                .andExpect(jsonPath("$.status").value("PLACED"))
                .andExpect(jsonPath("$.subtotal").value(179.98))
                .andExpect(jsonPath("$.shipping").value(15.00))
                .andExpect(jsonPath("$.total").value(194.98))
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].productId").value(productId.toString()))
                .andExpect(jsonPath("$.items[0].productName").value("Gaming Mouse"))
                .andExpect(jsonPath("$.items[0].unitPrice").value(89.99))
                .andExpect(jsonPath("$.items[0].quantity").value(2));

        verifyDownstreamRequests();
    }

    @Test
    @Timeout(10)
    void shouldRejectOrderWhenCustomerDoesNotExist() throws Exception {

        UUID customerId = UUID.randomUUID();

        String token =
                JwtTestFactory.createToken(
                        customerId, "test@example.com", jwtSecret, Duration.ofHours(1));

        OrderRequest request =
                new OrderRequest(
                        List.of(new OrderItemRequest(productId, 2)),
                        shippingAddress());

        // Customer does not exist
        mockWebServer.enqueue(
                new MockResponse()
                        .setResponseCode(404)
                        .addHeader("Content-Length", "0"));

        mockMvc.perform(
                        post("/api/orders")
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());

        RecordedRequest recordedRequest = mockWebServer.takeRequest();

        assertEquals("HEAD", recordedRequest.getMethod());
        assertTrue(recordedRequest.getPath().startsWith("/api/customers/"));
    }

    @Test
    @Timeout(10)
    void shouldRejectOrderWhenProductDoesNotExist() throws Exception {

        UUID customerId = UUID.randomUUID();

        String token =
                JwtTestFactory.createToken(
                        customerId, "test@example.com", jwtSecret, Duration.ofHours(1));

        OrderRequest request =
                new OrderRequest(
                        List.of(new OrderItemRequest(productId, 2)),
                        shippingAddress());

        // Customer exists
        mockWebServer.enqueue(
                new MockResponse()
                        .setResponseCode(200)
                        .addHeader("Content-Length", "0"));

        // Inventory reservation succeeds
        mockWebServer.enqueue(
                new MockResponse()
                        .setResponseCode(200)
                        .addHeader("Content-Length", "0"));

        // Product does not exist
        mockWebServer.enqueue(
                new MockResponse()
                        .setResponseCode(404)
                        .addHeader("Content-Length", "0"));

        mockMvc.perform(
                        post("/api/orders")
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());

        // First downstream call: customer validation
        RecordedRequest customerRequest = mockWebServer.takeRequest();

        assertEquals("HEAD", customerRequest.getMethod());
        assertTrue(customerRequest.getPath().startsWith("/api/customers/"));

        // Second downstream call: inventory reservation
        RecordedRequest inventoryRequest = mockWebServer.takeRequest();

        assertTrue(inventoryRequest.getPath().startsWith("/api/inventory/"));
        assertTrue(inventoryRequest.getPath().endsWith("/reserve"));

        // Third downstream call: product lookup
        RecordedRequest productRequest = mockWebServer.takeRequest();

        assertEquals("GET", productRequest.getMethod());
        assertTrue(productRequest.getPath().startsWith("/api/products/"));
    }

    @Test
    @Timeout(10)
    void shouldRejectOrderWhenInventoryIsInsufficient() throws Exception {
    
        UUID customerId = UUID.randomUUID();
    
        String token =
                JwtTestFactory.createToken(
                        customerId, "test@example.com", jwtSecret, Duration.ofHours(1));
    
        OrderRequest request =
                new OrderRequest(
                        List.of(new OrderItemRequest(productId, 10)),
                        shippingAddress());
    
        // Customer exists
        mockWebServer.enqueue(
                new MockResponse()
                        .setResponseCode(200)
                        .addHeader("Content-Length", "0"));
    
        // Inventory is insufficient
        mockWebServer.enqueue(
                new MockResponse()
                        .setResponseCode(409)
                        .addHeader("Content-Length", "0"));
    
        mockMvc.perform(
                        post("/api/orders")
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    
        // Customer validation
        RecordedRequest customerRequest = mockWebServer.takeRequest();
    
        assertEquals("HEAD", customerRequest.getMethod());
        assertTrue(customerRequest.getPath().startsWith("/api/customers/"));
    
        // Inventory reservation
        RecordedRequest inventoryRequest = mockWebServer.takeRequest();
    
        assertEquals("POST", inventoryRequest.getMethod());
        assertTrue(inventoryRequest.getPath().startsWith("/api/inventory/"));
        assertTrue(inventoryRequest.getPath().endsWith("/reserve"));
    }

    @Test
    @Timeout(10)
    void shouldRejectOrderWhenItemsAreEmpty() throws Exception {

        UUID customerId = UUID.randomUUID();

        String token =
                JwtTestFactory.createToken(
                        customerId, "test@example.com", jwtSecret, Duration.ofHours(1));

        OrderRequest request =
                new OrderRequest(
                        List.of(),
                        shippingAddress());

        mockMvc.perform(
                        post("/api/orders")
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(jsonPath("$.message").value("items: Items must not be empty"))
                .andExpect(status().isBadRequest());

    }

    @Test
    @Timeout(10)
    void shouldRejectOrderWhenItemQuantityIsInvalid() throws Exception {

        UUID customerId = UUID.randomUUID();

        String token =
                JwtTestFactory.createToken(
                        customerId, "test@example.com", jwtSecret, Duration.ofHours(1));

        OrderRequest request =
                new OrderRequest(
                        List.of(new OrderItemRequest(productId, 0)),
                        shippingAddress());

        mockMvc.perform(
                        post("/api/orders")
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Timeout(10)
    void shouldRejectOrderWhenShippingAddressIsMissing() throws Exception {

        UUID customerId = UUID.randomUUID();

        String token =
                JwtTestFactory.createToken(
                        customerId, "test@example.com", jwtSecret, Duration.ofHours(1));

        OrderRequest request =
                new OrderRequest(
                        List.of(new OrderItemRequest(productId, 2)),
                        null);

        int requestCountBefore = mockWebServer.getRequestCount();                 

        mockMvc.perform(
                        post("/api/orders")
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        assertEquals(requestCountBefore, mockWebServer.getRequestCount());   
    }

    @Test
    @Timeout(10)
    void shouldRejectOrderWhenShippingAddressIsInvalid() throws Exception {

        UUID customerId = UUID.randomUUID();

        String token =
                JwtTestFactory.createToken(
                        customerId, "test@example.com", jwtSecret, Duration.ofHours(1));

        ShippingAddressRequest invalidAddress =
                ShippingAddressRequest.builder()
                        .addressLine1("1 Main St")
                        .city("Auckland")
                        .postcode("")
                        .country("NZ")
                        .build();

        OrderRequest request =
                new OrderRequest(
                        List.of(new OrderItemRequest(productId, 2)),
                        invalidAddress);

        int requestCountBefore = mockWebServer.getRequestCount();                

        mockMvc.perform(
                        post("/api/orders")
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        assertEquals(requestCountBefore, mockWebServer.getRequestCount());        
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

    private ShippingAddressRequest shippingAddress() {
        return ShippingAddressRequest.builder()
                .addressLine1("1 Main St")
                .city("Auckland")
                .postcode("1010")
                .country("NZ")
                .build();
    }

    private String createToken(UUID customerId) {
        return JwtTestFactory.createToken(
                customerId,
                "test@example.com",
                jwtSecret,
                Duration.ofHours(1));
    }
    
    private OrderRequest createOrderRequest() {
        List<OrderItemRequest> itemRequests =
                List.of(new OrderItemRequest(productId, 2));
    
        return new OrderRequest(itemRequests, shippingAddress());
    }

    private void enqueueSuccessfulOrderDependencies() throws JsonProcessingException {
        mockWebServer.enqueue(
                new MockResponse()
                        .setResponseCode(200)
                        .addHeader("Content-Length", "0"));
    
        mockWebServer.enqueue(
                new MockResponse()
                        .setResponseCode(200)
                        .addHeader("Content-Length", "0"));
    
        mockWebServer.enqueue(
                new MockResponse()
                        .setResponseCode(200)
                        .setBody(objectMapper.writeValueAsString(productSnapshot()))
                        .addHeader("Content-Type", "application/json"));
    }

    private void verifyDownstreamRequests() throws InterruptedException {
        RecordedRequest customerRequest = mockWebServer.takeRequest();
        RecordedRequest inventoryRequest = mockWebServer.takeRequest();
        RecordedRequest productRequest = mockWebServer.takeRequest();
    
        assertEquals("HEAD", customerRequest.getMethod());
        assertTrue(customerRequest.getPath().startsWith("/api/customers/"));
    
        assertEquals("POST", inventoryRequest.getMethod());
        assertTrue(inventoryRequest.getPath().startsWith("/api/inventory/"));
        assertTrue(inventoryRequest.getPath().endsWith("/reserve"));
    
        assertEquals("GET", productRequest.getMethod());
        assertTrue(productRequest.getPath().startsWith("/api/products/"));
    }
}
