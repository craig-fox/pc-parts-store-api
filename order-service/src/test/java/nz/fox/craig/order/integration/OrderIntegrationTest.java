package nz.fox.craig.order.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import nz.fox.craig.order.dto.client.ProductSnapshot;
import nz.fox.craig.order.dto.request.OrderItemRequest;
import nz.fox.craig.order.dto.request.OrderRequest;
import nz.fox.craig.order.dto.request.ShippingAddressRequest;
import nz.fox.craig.order.fixture.OrderFixtures;
import nz.fox.craig.order.repository.OrderRepository;
import nz.fox.craig.test.AbstractPostgresTest;
import nz.fox.craig.test.JwtTestFactory;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.QueueDispatcher;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.RecordedRequest;

@SpringBootTest
@AutoConfigureMockMvc
class OrderIntegrationTest extends AbstractPostgresTest {

    @Autowired private MockMvc mockMvc;

    @Autowired private ObjectMapper objectMapper;

    @Autowired private OrderRepository orderRepository;

    static MockWebServer mockWebServer;

    private static String idempotencyKey = UUID.randomUUID().toString();

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

    @BeforeEach
    void resetMockWebServer() {
        mockWebServer.setDispatcher(new QueueDispatcher());
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

        OrderRequest request = OrderFixtures.anOrderRequest();

        enqueueSuccessfulOrderDependencies();
      
        mockMvc.perform(
                post("/api/orders")
                        .header("Authorization", "Bearer " + token)
                        .header("Idempotency-Key", idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.customerId").value(customerId.toString()))
                .andExpect(jsonPath("$.orderDate").exists())
                .andExpect(jsonPath("$.status").value("PLACED"))
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].productId").value(productId.toString()));
        
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
                new OrderRequest(List.of(new OrderItemRequest(productId, 2)), shippingAddress());

        // Customer does not exist
        mockWebServer.enqueue(
                new MockResponse().setResponseCode(404).addHeader("Content-Length", "0"));

        mockMvc.perform(
                        post("/api/orders")
                                .header("Authorization", "Bearer " + token)
                                .header("Idempotency-Key", idempotencyKey)
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
                new OrderRequest(List.of(new OrderItemRequest(productId, 2)), shippingAddress());

        // Customer exists
        mockWebServer.enqueue(
                new MockResponse().setResponseCode(200).addHeader("Content-Length", "0"));

        // Inventory reservation succeeds
        mockWebServer.enqueue(
                new MockResponse().setResponseCode(200).addHeader("Content-Length", "0"));

        // Product does not exist
        mockWebServer.enqueue(
                new MockResponse().setResponseCode(404).addHeader("Content-Length", "0"));

        // Inventory reservation is released after product lookup fails
        mockWebServer.enqueue(
                new MockResponse().setResponseCode(200).addHeader("Content-Length", "0"));

        mockMvc.perform(
                        post("/api/orders")
                                .header("Authorization", "Bearer " + token)
                                .header("Idempotency-Key", idempotencyKey)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());

        //First downstream call: customer validation
        RecordedRequest customerRequest = mockWebServer.takeRequest();

        assertEquals("HEAD", customerRequest.getMethod());
        assertTrue(customerRequest.getPath().startsWith("/api/customers/"));

        //Second downstream call: inventory reservation
        RecordedRequest inventoryRequest = mockWebServer.takeRequest();

        assertTrue(inventoryRequest.getPath().startsWith("/api/inventory/"));
        assertTrue(inventoryRequest.getPath().endsWith("/reserve"));

        //Third downstream call: product lookup
        RecordedRequest productRequest = mockWebServer.takeRequest();

        assertEquals("GET", productRequest.getMethod());
        assertTrue(productRequest.getPath().startsWith("/api/products/"));

        RecordedRequest releaseRequest = mockWebServer.takeRequest();

        assertEquals("POST", releaseRequest.getMethod());
        assertTrue(releaseRequest.getPath().startsWith("/api/inventory/"));
        assertTrue(releaseRequest.getPath().endsWith("/release"));
    }

    @Test
    @Timeout(10)
    void shouldRejectOrderWhenInventoryIsInsufficient() throws Exception {

        UUID customerId = UUID.randomUUID();
        long initialOrderCount = orderRepository.count();

        String token =
                JwtTestFactory.createToken(
                        customerId, "test@example.com", jwtSecret, Duration.ofHours(1));

        OrderRequest request =
                new OrderRequest(List.of(new OrderItemRequest(productId, 10)), shippingAddress());

        // Customer exists
        mockWebServer.enqueue(
                new MockResponse().setResponseCode(200).addHeader("Content-Length", "0"));

        // Inventory is insufficient
        mockWebServer.enqueue(
                new MockResponse().setResponseCode(409).addHeader("Content-Length", "0"));

        mockMvc.perform(
                        post("/api/orders")
                                .header("Authorization", "Bearer " + token)
                                .header("Idempotency-Key", idempotencyKey)
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
        assertThat(orderRepository.count()).isEqualTo(initialOrderCount);
    }

    @Test
    @Timeout(10)
    void shouldRejectOrderWhenItemsAreEmpty() throws Exception {

        UUID customerId = UUID.randomUUID();

        String token =
                JwtTestFactory.createToken(
                        customerId, "test@example.com", jwtSecret, Duration.ofHours(1));

        OrderRequest request = new OrderRequest(List.of(), shippingAddress());

        mockMvc.perform(
                        post("/api/orders")
                                .header("Authorization", "Bearer " + token)
                                .header("Idempotency-Key", idempotencyKey)
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
                new OrderRequest(List.of(new OrderItemRequest(productId, 0)), shippingAddress());

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

        OrderRequest request = new OrderRequest(List.of(new OrderItemRequest(productId, 2)), null);

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
                new OrderRequest(List.of(new OrderItemRequest(productId, 2)), invalidAddress);

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
    void shouldRejectOrderWithoutJwt() throws Exception {
        OrderRequest request = OrderFixtures.anOrderRequest();

        int requestCountBefore = mockWebServer.getRequestCount();

        mockMvc.perform(
                        post("/api/orders")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());

        assertEquals(requestCountBefore, mockWebServer.getRequestCount());
    }

    @Test
    @Timeout(10)
    void shouldReleasePreviouslyReservedStockWhenLaterReservationFails() throws Exception {
        UUID customerId = UUID.randomUUID();
        UUID firstProductId = UUID.randomUUID();
        UUID secondProductId = UUID.randomUUID();

        long initialOrderCount = orderRepository.count();

        String token =
                JwtTestFactory.createToken(
                        customerId, "test@example.com", jwtSecret, Duration.ofHours(1));

        OrderRequest request =
                new OrderRequest(
                        List.of(
                                new OrderItemRequest(firstProductId, 2),
                                new OrderItemRequest(secondProductId, 1)),
                        shippingAddress());

        enqueueResponses();
        mockMvc.perform(
                        post("/api/orders")
                                .header("Authorization", "Bearer " + token)
                                .header("Idempotency-Key", idempotencyKey)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());

        // Customer validation
        RecordedRequest customerRequest = mockWebServer.takeRequest();

        assertEquals("HEAD", customerRequest.getMethod());
        assertTrue(customerRequest.getPath().startsWith("/api/customers/"));

        // First reservation
        RecordedRequest firstReservation = mockWebServer.takeRequest();

        assertEquals("POST", firstReservation.getMethod());
        assertTrue(firstReservation.getPath().endsWith("/reserve"));
        assertTrue(firstReservation.getPath().contains(firstProductId.toString()));

        // Failed second reservation
        RecordedRequest secondReservation = mockWebServer.takeRequest();

        assertEquals("POST", secondReservation.getMethod());
        assertTrue(secondReservation.getPath().endsWith("/reserve"));
        assertTrue(secondReservation.getPath().contains(secondProductId.toString()));

        // Compensation for first reservation
        RecordedRequest releaseRequest = mockWebServer.takeRequest();

        assertEquals("POST", releaseRequest.getMethod());
        assertTrue(releaseRequest.getPath().endsWith("/release"));
        assertTrue(releaseRequest.getPath().contains(firstProductId.toString()));

        assertThat(orderRepository.count()).isEqualTo(initialOrderCount);
    }


    @Test
    void shouldRejectOrderCreationWithoutIdempotencyKey() throws Exception {
        UUID customerId = UUID.randomUUID();
        OrderRequest request = new OrderRequest(List.of(new OrderItemRequest(productId, 2)), null);

        String token =
                JwtTestFactory.createToken(
                        customerId, "test@example.com", jwtSecret, Duration.ofHours(1));
        
        mockMvc.perform(
                post("/api/orders")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Timeout(10)
    void shouldReturnExistingOrderForSameIdempotencyKeyAndRequest() throws Exception {
        UUID customerId = UUID.randomUUID();
        String token = createToken(customerId);
        String idempotencyKey = UUID.randomUUID().toString();

        OrderRequest request = OrderFixtures.anOrderRequest();

        enqueueSuccessfulOrderDependencies();

        MvcResult firstResult =
                mockMvc.perform(
                                post("/api/orders")
                                        .header("Authorization", "Bearer " + token)
                                        .header("Idempotency-Key", idempotencyKey)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(request)))
                        .andExpect(status().isCreated())
                        .andExpect(jsonPath("$.id").exists())
                        .andReturn();

        verifyDownstreamRequests();

        UUID orderId =
            UUID.fromString(
                    objectMapper
                            .readTree(firstResult.getResponse().getContentAsString())
                            .get("id")
                            .asText());

        MvcResult secondResult =
                mockMvc.perform(
                                post("/api/orders")
                                        .header("Authorization", "Bearer " + token)
                                        .header("Idempotency-Key", idempotencyKey)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(request)))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.id").value(orderId.toString()))
                        .andReturn();
        UUID secondOrderId =
            UUID.fromString(
                objectMapper
                        .readTree(secondResult.getResponse().getContentAsString())
                        .get("id")
                        .asText());

        assertThat(secondOrderId).isEqualTo(orderId);
    }

    @Test
    @Timeout(10)
    void shouldNotReserveInventoryAgainForIdempotentRetry() throws Exception {
        UUID customerId = UUID.randomUUID();
        String token = createToken(customerId);
        String idempotencyKey = UUID.randomUUID().toString();
    
        OrderRequest request = OrderFixtures.anOrderRequest();
        String requestJson = objectMapper.writeValueAsString(request);
    
        enqueueSuccessfulOrderDependencies();
        int requestCountBefore = mockWebServer.getRequestCount();
    
        mockMvc.perform(
                        post("/api/orders")
                                .header("Authorization", "Bearer " + token)
                                .header("Idempotency-Key", idempotencyKey)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestJson))
                .andExpect(status().isCreated());
    
        verifyDownstreamRequests();
    
        int requestCountAfterFirstRequest = mockWebServer.getRequestCount();

        assertThat(requestCountAfterFirstRequest - requestCountBefore)
                .isEqualTo(3);
    
        mockMvc.perform(
                        post("/api/orders")
                                .header("Authorization", "Bearer " + token)
                                .header("Idempotency-Key", idempotencyKey)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestJson))
                .andExpect(status().isOk());
    
        assertThat(mockWebServer.getRequestCount())
                .isEqualTo(requestCountAfterFirstRequest);
    }

    @Test
    @Timeout(10)
    void shouldRejectReuseOfIdempotencyKeyWithDifferentRequest() throws Exception {
        UUID customerId = UUID.randomUUID();
        String token = createToken(customerId);
        String idempotencyKey = UUID.randomUUID().toString();

        OrderRequest firstRequest = OrderFixtures.anOrderRequest();

        OrderRequest secondRequest =
                OrderFixtures.anOrderRequest(
                        List.of(
                                OrderItemRequest.builder()
                                        .productId(productId)
                                        .quantity(2)
                                        .build()));

        String firstRequestJson = objectMapper.writeValueAsString(firstRequest);
        String secondRequestJson = objectMapper.writeValueAsString(secondRequest);

        enqueueSuccessfulOrderDependencies();

        mockMvc.perform(
                        post("/api/orders")
                                .header("Authorization", "Bearer " + token)
                                .header("Idempotency-Key", idempotencyKey)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(firstRequestJson))
                .andExpect(status().isCreated());

        verifyDownstreamRequests();

        int requestCountAfterFirstRequest = mockWebServer.getRequestCount();

        mockMvc.perform(
                        post("/api/orders")
                                .header("Authorization", "Bearer " + token)
                                .header("Idempotency-Key", idempotencyKey)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(secondRequestJson))
                .andExpect(status().isConflict())
                .andExpect(
                        jsonPath("$.message")
                                .value(
                                        "Idempotency key has already been used with a different request: "
                                                + idempotencyKey));

        assertThat(mockWebServer.getRequestCount())
                .isEqualTo(requestCountAfterFirstRequest);
    }

    @Test
    @Timeout(10)
    void shouldAllowSameIdempotencyKeyForDifferentCustomer() throws Exception {
        UUID firstCustomerId = UUID.randomUUID();
        UUID secondCustomerId = UUID.randomUUID();
        String firstToken = createToken(firstCustomerId);
        String secondToken = createToken(secondCustomerId);
        String idempotencyKey = UUID.randomUUID().toString();

        OrderRequest request = OrderFixtures.anOrderRequest();
        String requestJson = objectMapper.writeValueAsString(request);

        // First customer's dependencies
        enqueueSuccessfulOrderDependencies();

        MvcResult firstResult =
                mockMvc.perform(
                                post("/api/orders")
                                        .header("Authorization", "Bearer " + firstToken)
                                        .header("Idempotency-Key", idempotencyKey)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(requestJson))
                        .andExpect(status().isCreated())
                        .andExpect(jsonPath("$.id").exists())
                        .andExpect(jsonPath("$.customerId").value(firstCustomerId.toString()))
                        .andReturn();

        verifyDownstreamRequests();

        UUID firstOrderId =
                UUID.fromString(
                        objectMapper
                                .readTree(firstResult.getResponse().getContentAsString())
                                .get("id")
                                .asText());

        // Second customer's dependencies
        enqueueSuccessfulOrderDependencies();

        MvcResult secondResult =
                mockMvc.perform(
                                post("/api/orders")
                                        .header("Authorization", "Bearer " + secondToken)
                                        .header("Idempotency-Key", idempotencyKey)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(requestJson))
                        .andExpect(status().isCreated())
                        .andExpect(jsonPath("$.id").exists())
                        .andExpect(jsonPath("$.customerId").value(secondCustomerId.toString()))
                        .andReturn();

        verifyDownstreamRequests();

        UUID secondOrderId =
                UUID.fromString(
                        objectMapper
                                .readTree(secondResult.getResponse().getContentAsString())
                                .get("id")
                                .asText());

        assertThat(secondOrderId).isNotEqualTo(firstOrderId);
    }


    @Test
    @Timeout(10)
    void shouldCreateOnlyOneOrderForConcurrentRequestsWithSameIdempotencyKey()
                throws Exception {

        UUID customerId = UUID.randomUUID();
        String token = createToken(customerId);
        String idempotencyKey = UUID.randomUUID().toString();
        String requestJson =
                objectMapper.writeValueAsString(OrderFixtures.anOrderRequest());

        long initialOrderCount = orderRepository.count();
        int initialRequestCount = mockWebServer.getRequestCount();

        Dispatcher originalDispatcher = mockWebServer.getDispatcher();
        mockWebServer.setDispatcher(concurrentOrderDispatcher());

        try {
                List<Integer> statuses =
                        executeConcurrentOrderRequests(token, idempotencyKey, requestJson);

                assertConcurrentOrderResults(
                        statuses, initialOrderCount, initialRequestCount);
                assertCompensatedReservation();
        } finally {
                restoreDispatcher(originalDispatcher);
        }
    }

    private Dispatcher concurrentOrderDispatcher() {
        return new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) {
                String path = request.getPath();
    
                if ("HEAD".equals(request.getMethod())
                        && path.startsWith("/api/customers/")) {
                    return new MockResponse().setResponseCode(200);
                }
    
                if ("POST".equals(request.getMethod())
                        && path.endsWith("/reserve")) {
                    return new MockResponse().setResponseCode(200);
                }
    
                if ("POST".equals(request.getMethod())
                        && path.endsWith("/release")) {
                    return new MockResponse().setResponseCode(200);
                }
    
                if ("GET".equals(request.getMethod())
                        && path.startsWith("/api/products/")) {
                    return productResponse();
                }
    
                return new MockResponse().setResponseCode(404);
            }
        };
    }

    private MockResponse productResponse() {
        try {
            return new MockResponse()
                    .setResponseCode(200)
                    .setBody(objectMapper.writeValueAsString(productSnapshot()))
                    .addHeader("Content-Type", "application/json");
        } catch (JsonProcessingException ex) {
            throw new AssertionError(ex);
        }
    }

    private List<Integer> executeConcurrentOrderRequests(
        String token, String idempotencyKey, String requestJson) throws Exception {

        CountDownLatch startLatch = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);

        Callable<Integer> createOrder =
                () -> executeOrderRequest(
                        startLatch, token, idempotencyKey, requestJson);

        try {
                List<Future<Integer>> futures =
                        List.of(
                                executor.submit(createOrder),
                                executor.submit(createOrder));

                startLatch.countDown();

                return futures.stream()
                        .map(this::getStatus)
                        .toList();
        } finally {
                executor.shutdown();
                assertThat(executor.awaitTermination(5, TimeUnit.SECONDS)).isTrue();
        }
    }

    private int executeOrderRequest(
        CountDownLatch startLatch,
        String token,
        String idempotencyKey,
        String requestJson)
        throws Exception {

        startLatch.await();

        return mockMvc.perform(
                        post("/api/orders")
                                .header("Authorization", "Bearer " + token)
                                .header("Idempotency-Key", idempotencyKey)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestJson))
                .andReturn()
                .getResponse()
                .getStatus();
        }
    

    private void assertConcurrentOrderResults(
        List<Integer> statuses,
        long initialOrderCount,
        int initialRequestCount)
        throws InterruptedException {

        assertThat(statuses)
                .contains(HttpStatus.CREATED.value())
                .contains(HttpStatus.OK.value());

        assertThat(orderRepository.count())
                .isEqualTo(initialOrderCount + 1);

        assertThat(mockWebServer.getRequestCount())
                .isEqualTo(initialRequestCount + 7);
    }


    private void assertCompensatedReservation() throws InterruptedException {

        List<RecordedRequest> requests = new ArrayList<>();
        
        for (int i = 0; i < 7; i++) {
                requests.add(mockWebServer.takeRequest());
        }
        
        long reserveRequests =
                requests.stream()
                        .filter(request -> "POST".equals(request.getMethod()))
                        .filter(request -> request.getPath().endsWith("/reserve"))
                        .count();
        
        long releaseRequests =
                requests.stream()
                        .filter(request -> "POST".equals(request.getMethod()))
                        .filter(request -> request.getPath().endsWith("/release"))
                        .count();
        
        assertThat(reserveRequests).isEqualTo(2);
        assertThat(releaseRequests).isEqualTo(1);
    }

    private void restoreDispatcher(Dispatcher originalDispatcher) {
        mockWebServer.setDispatcher(originalDispatcher);
    }
        

    private int getStatus(Future<Integer> result) {
        try {
            return result.get();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new AssertionError("Test interrupted", ex);
        } catch (ExecutionException ex) {
            throw new AssertionError("Concurrent order request failed", ex);
        }
    }

    private void enqueueResponses() {
        // Customer exists
        mockWebServer.enqueue(
                new MockResponse().setResponseCode(200).addHeader("Content-Length", "0"));

        // First inventory reservation succeeds
        mockWebServer.enqueue(
                new MockResponse().setResponseCode(200).addHeader("Content-Length", "0"));

        // Second inventory reservation fails
        mockWebServer.enqueue(
                new MockResponse().setResponseCode(409).addHeader("Content-Length", "0"));

        // First reservation is released
        mockWebServer.enqueue(
                new MockResponse().setResponseCode(200).addHeader("Content-Length", "0"));

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
                customerId, "test@example.com", jwtSecret, Duration.ofHours(1));
    }


    private void enqueueSuccessfulOrderDependencies() throws JsonProcessingException {
        mockWebServer.enqueue(
                new MockResponse().setResponseCode(200).addHeader("Content-Length", "0"));

        mockWebServer.enqueue(
                new MockResponse().setResponseCode(200).addHeader("Content-Length", "0"));

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
