package nz.fox.craig.order.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.core.JsonProcessingException;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import nz.fox.craig.api.ShippingMethod;
import nz.fox.craig.order.dto.client.ProductSnapshot;
import nz.fox.craig.order.dto.request.OrderItemRequest;
import nz.fox.craig.order.dto.request.OrderRequest;
import nz.fox.craig.order.dto.request.ShippingAddressRequest;
import nz.fox.craig.order.dto.response.ShippingAddressResponse;
import nz.fox.craig.order.dto.response.ShippingQuoteResponse;
import nz.fox.craig.order.fixture.OrderFixture;
import nz.fox.craig.order.repository.OrderRepository;
import nz.fox.craig.test.AbstractPostgresTest;
import nz.fox.craig.test.JwtTestFactory;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.QueueDispatcher;
import okhttp3.mockwebserver.RecordedRequest;
import tools.jackson.databind.json.JsonMapper;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import okhttp3.mockwebserver.Dispatcher;

@SpringBootTest
@AutoConfigureMockMvc
class OrderIntegrationTest extends AbstractPostgresTest {

    private static final int SUCCESSFUL_ORDER_DOWNSTREAM_REQUEST_COUNT = 5;
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String IDEMPOTENCY_HEADER = "Idempotency-Key";

    @Autowired private MockMvc mockMvc;

    @Autowired private JsonMapper objectMapper;

    @Autowired private OrderRepository orderRepository;

    static MockWebServer mockWebServer;

    @Value("${jwt.secret}")
    private String jwtSecret;

    private final UUID productId = UUID.fromString("1b0d0fa6-52e1-4acd-8286-892bc29f8b3a");

    private final BigDecimal standardRate = BigDecimal.valueOf(15.00);
    private final BigDecimal expressRate = BigDecimal.valueOf(25.00);

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        registry.add("services.customer.base-url", () -> mockWebServer.url("/").toString());
        registry.add("services.product.base-url", () -> mockWebServer.url("/").toString());
        registry.add("services.inventory.base-url", () -> mockWebServer.url("/").toString());
        registry.add("services.payment.base-url", () -> mockWebServer.url("/").toString());
        registry.add("services.shipping.base-url", () -> mockWebServer.url("/").toString());
    }
    
    @BeforeAll
    static void startServer() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();
    }

    @BeforeEach
    void resetMockWebServer() throws InterruptedException {
        mockWebServer.setDispatcher(new QueueDispatcher());
        drainRecordedRequests();
    }
    
    @AfterAll
    static void stopServer() throws IOException {
        mockWebServer.shutdown();
    }

    @Test
    void shouldCreateOrderForExistingCustomer() throws Exception {
        UUID customerId = UUID.randomUUID();
        String token = createToken(customerId);
        String idempotencyKey = UUID.randomUUID().toString();

        OrderRequest request = OrderFixture.anOrderRequest();

        enqueueSuccessfulOrderDependencies(ShippingMethod.STANDARD, standardRate);
      
        mockMvc.perform(
                createOrderRequest(token, idempotencyKey, request))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.customerId").value(customerId.toString()))
                .andExpect(jsonPath("$.orderDate").exists())
                .andExpect(jsonPath("$.status").value("PAID"))
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].productId").value(productId.toString()))
                .andExpect(jsonPath("$.shipping").value(15.00))
                .andExpect(jsonPath("$.total").value(104.99));
        
        verifySuccessfulDownstreamRequests();
    }

    @Test
    void shouldRejectOrderWhenCustomerDoesNotExist() throws Exception {

        UUID customerId = UUID.randomUUID();
        String idempotencyKey = UUID.randomUUID().toString();

        String token =
                JwtTestFactory.createToken(
                        customerId, "test@example.com", jwtSecret, Duration.ofHours(1));

        OrderRequest request =
                new OrderRequest(List.of(new OrderItemRequest(productId, 2)), shippingAddress(), ShippingMethod.STANDARD);

        // Customer does not exist
        mockWebServer.enqueue(
                new MockResponse().setResponseCode(404).addHeader("Content-Length", "0"));

        mockMvc.perform(
                createOrderRequest(token, idempotencyKey, request))
        .andExpect(status().isNotFound());

        RecordedRequest recordedRequest = mockWebServer.takeRequest();

        assertThat(recordedRequest.getMethod()).isEqualTo("HEAD");
        assertThat(recordedRequest.getPath()).startsWith("/api/customers/");
    }

    @Test
    void shouldRejectOrderWhenProductDoesNotExist() throws Exception {

        UUID customerId = UUID.randomUUID();
        String idempotencyKey = UUID.randomUUID().toString();

        String token =
                JwtTestFactory.createToken(
                        customerId, "test@example.com", jwtSecret, Duration.ofHours(1));

        OrderRequest request =
                new OrderRequest(List.of(new OrderItemRequest(productId, 2)), shippingAddress(), ShippingMethod.STANDARD);

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
                createOrderRequest(token, idempotencyKey, request))
        .andExpect(status().isNotFound());

        //First downstream call: customer validation
        RecordedRequest customerRequest = mockWebServer.takeRequest();

        assertThat(customerRequest.getMethod()).isEqualTo("HEAD");
        assertThat(customerRequest.getPath()).startsWith("/api/customers/");
      
        //Second downstream call: inventory reservation
        RecordedRequest inventoryRequest = mockWebServer.takeRequest();

        assertThat(inventoryRequest.getPath()).startsWith("/api/inventory/");
        assertThat(inventoryRequest.getPath()).endsWith("/reserve");
      
        //Third downstream call: product lookup
        RecordedRequest productRequest = mockWebServer.takeRequest();

        assertThat(productRequest.getMethod()).isEqualTo("GET");
        assertThat(productRequest.getPath()).startsWith("/api/products/");
        

        RecordedRequest releaseRequest = mockWebServer.takeRequest();

        assertThat(releaseRequest.getMethod()).isEqualTo("POST");
        assertThat(releaseRequest.getPath()).startsWith("/api/inventory/");
        assertThat(releaseRequest.getPath()).endsWith("/release");
    }

    @Test
    void shouldRejectOrderWhenInventoryIsInsufficient() throws Exception {

        UUID customerId = UUID.randomUUID();
        long initialOrderCount = orderRepository.count();
        String idempotencyKey = UUID.randomUUID().toString();

        String token =
                JwtTestFactory.createToken(
                        customerId, "test@example.com", jwtSecret, Duration.ofHours(1));

        OrderRequest request =
                new OrderRequest(List.of(new OrderItemRequest(productId, 10)), shippingAddress(), ShippingMethod.STANDARD);

        // Customer exists
        mockWebServer.enqueue(
                new MockResponse().setResponseCode(200).addHeader("Content-Length", "0"));

        // Inventory is insufficient
        mockWebServer.enqueue(
                new MockResponse().setResponseCode(409).addHeader("Content-Length", "0"));

        mockMvc.perform(
                createOrderRequest(token, idempotencyKey, request))
        .andExpect(status().isConflict());

        // Customer validation
        RecordedRequest customerRequest = mockWebServer.takeRequest();

        assertThat(customerRequest.getMethod()).isEqualTo("HEAD");
        assertThat(customerRequest.getPath()).startsWith("/api/customers/");


        // Inventory reservation
        RecordedRequest inventoryRequest = mockWebServer.takeRequest();

        assertThat(inventoryRequest.getMethod()).isEqualTo("POST");
        assertThat(inventoryRequest.getPath()).startsWith("/api/inventory/");
        assertThat(inventoryRequest.getPath()).endsWith("/reserve");
        assertThat(orderRepository.count()).isEqualTo(initialOrderCount);
    }


    @Test
    void shouldRejectOrderWithoutJwt() throws Exception {
        OrderRequest request = OrderFixture.anOrderRequest();

        int requestCountBefore = mockWebServer.getRequestCount();

        mockMvc.perform(
                        post("/api/orders")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
        assertThat(mockWebServer.getRequestCount()).isEqualTo(requestCountBefore);
    }

    @Test
    @Timeout(10)
    void shouldReleasePreviouslyReservedStockWhenLaterReservationFails() throws Exception {
        UUID customerId = UUID.randomUUID();
        UUID firstProductId = UUID.randomUUID();
        UUID secondProductId = UUID.randomUUID();
        String idempotencyKey = UUID.randomUUID().toString();

        long initialOrderCount = orderRepository.count();

        String token =
                JwtTestFactory.createToken(
                        customerId, "test@example.com", jwtSecret, Duration.ofHours(1));

        OrderRequest request =
                new OrderRequest(
                        List.of(
                                new OrderItemRequest(firstProductId, 2),
                                new OrderItemRequest(secondProductId, 1)),
                        shippingAddress(), ShippingMethod.STANDARD);

        enqueueResponses();
        mockMvc.perform(
                createOrderRequest(token, idempotencyKey, request))
                .andExpect(status().isConflict());

        // Customer validation
        RecordedRequest customerRequest = mockWebServer.takeRequest();

        assertThat(customerRequest.getMethod()).isEqualTo("HEAD");
        assertThat(customerRequest.getPath()).startsWith("/api/customers/");

        // First reservation
        RecordedRequest firstReservation = mockWebServer.takeRequest();

        assertThat(firstReservation.getMethod()).isEqualTo("POST");
        assertThat(firstReservation.getPath()).endsWith("/reserve");
        assertThat(firstReservation.getPath()).contains(firstProductId.toString());

        // Failed second reservation
        RecordedRequest secondReservation = mockWebServer.takeRequest();

        assertThat(secondReservation.getMethod()).isEqualTo("POST");
        assertThat(secondReservation.getPath()).endsWith("/reserve");
        assertThat(secondReservation.getPath()).contains(secondProductId.toString());
        
        // Compensation for first reservation
        RecordedRequest releaseRequest = mockWebServer.takeRequest();

        assertThat(releaseRequest.getMethod()).isEqualTo("POST");
        assertThat(releaseRequest.getPath()).endsWith("/release");
        assertThat(releaseRequest.getPath()).contains(firstProductId.toString());
        assertThat(orderRepository.count()).isEqualTo(initialOrderCount);
    }


    @Test
    void shouldRejectOrderCreationWithoutIdempotencyKey() throws Exception {
        UUID customerId = UUID.randomUUID();
        OrderRequest request = new OrderRequest(
                List.of(new OrderItemRequest(productId, 2)), null, ShippingMethod.STANDARD);

        String token =
                JwtTestFactory.createToken(
                        customerId, "test@example.com", jwtSecret, Duration.ofHours(1));
        
        mockMvc.perform(
                post("/api/orders")
                        .header(AUTHORIZATION_HEADER, "Bearer " + token)
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

        OrderRequest request = OrderFixture.anOrderRequest();

        enqueueSuccessfulOrderDependencies(ShippingMethod.STANDARD, standardRate);

        MvcResult firstResult =
                mockMvc.perform(
                        createOrderRequest(token, idempotencyKey, request))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.id").exists())
                                .andReturn();

        verifySuccessfulDownstreamRequests();

        UUID orderId =
            UUID.fromString(
                    objectMapper
                            .readTree(firstResult.getResponse().getContentAsString())
                            .get("id")
                            .asText());

        MvcResult secondResult =
                mockMvc.perform(
                        createOrderRequest(token, idempotencyKey, request))
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
    
        OrderRequest request = OrderFixture.anOrderRequest();
        String requestJson = objectMapper.writeValueAsString(request);
    
        enqueueSuccessfulOrderDependencies(ShippingMethod.STANDARD, standardRate);
        int requestCountBefore = mockWebServer.getRequestCount();
    
        mockMvc.perform(
                        post("/api/orders")
                                .header(AUTHORIZATION_HEADER, "Bearer " + token)
                                .header(IDEMPOTENCY_HEADER, idempotencyKey)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestJson))
                .andExpect(status().isCreated());
    
        verifySuccessfulDownstreamRequests();
    
        int requestCountAfterFirstRequest = mockWebServer.getRequestCount();

        assertThat(requestCountAfterFirstRequest - requestCountBefore)
                .isEqualTo(5);
    
        mockMvc.perform(
                        post("/api/orders")
                                .header(AUTHORIZATION_HEADER, "Bearer " + token)
                                .header(IDEMPOTENCY_HEADER, idempotencyKey)
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

        OrderRequest firstRequest = OrderFixture.anOrderRequest();

        OrderRequest secondRequest =
                OrderFixture.anOrderRequest(
                        ShippingMethod.STANDARD,
                        List.of(
                                OrderItemRequest.builder()
                                        .productId(productId)
                                        .quantity(2)
                                        .build()));

        String firstRequestJson = objectMapper.writeValueAsString(firstRequest);
        String secondRequestJson = objectMapper.writeValueAsString(secondRequest);

        enqueueSuccessfulOrderDependencies(ShippingMethod.STANDARD, standardRate);

        mockMvc.perform(
                        post("/api/orders")
                                .header(AUTHORIZATION_HEADER, "Bearer " + token)
                                .header(IDEMPOTENCY_HEADER, idempotencyKey)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(firstRequestJson))
                .andExpect(status().isCreated());

        verifySuccessfulDownstreamRequests();

        int requestCountAfterFirstRequest = mockWebServer.getRequestCount();

        mockMvc.perform(
                        post("/api/orders")
                                .header(AUTHORIZATION_HEADER, "Bearer " + token)
                                .header(IDEMPOTENCY_HEADER, idempotencyKey)
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

        OrderRequest request = OrderFixture.anOrderRequest();
        String requestJson = objectMapper.writeValueAsString(request);

        // First customer's dependencies
        enqueueSuccessfulOrderDependencies(ShippingMethod.STANDARD, standardRate);

        MvcResult firstResult =
                mockMvc.perform(
                                post("/api/orders")
                                        .header(AUTHORIZATION_HEADER, "Bearer " + firstToken)
                                        .header(IDEMPOTENCY_HEADER, idempotencyKey)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(requestJson))
                        .andExpect(status().isCreated())
                        .andExpect(jsonPath("$.id").exists())
                        .andExpect(jsonPath("$.customerId").value(firstCustomerId.toString()))
                        .andReturn();

        verifySuccessfulDownstreamRequests();

        UUID firstOrderId =
                UUID.fromString(
                        objectMapper
                                .readTree(firstResult.getResponse().getContentAsString())
                                .get("id")
                                .asText());

        // Second customer's dependencies
        enqueueSuccessfulOrderDependencies(ShippingMethod.STANDARD, standardRate);

        MvcResult secondResult =
                mockMvc.perform(
                                post("/api/orders")
                                        .header(AUTHORIZATION_HEADER, "Bearer " + secondToken)
                                        .header(IDEMPOTENCY_HEADER, idempotencyKey)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(requestJson))
                        .andExpect(status().isCreated())
                        .andExpect(jsonPath("$.id").exists())
                        .andExpect(jsonPath("$.customerId").value(secondCustomerId.toString()))
                        .andReturn();

        verifySuccessfulDownstreamRequests();

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
                objectMapper.writeValueAsString(OrderFixture.anOrderRequest());

        long initialOrderCount = orderRepository.count();
        int initialRequestCount = mockWebServer.getRequestCount();

        mockWebServer.setDispatcher(concurrentOrderDispatcher());

        try {
                List<Integer> statuses =
                        executeConcurrentOrderRequests(token, idempotencyKey, requestJson);

                assertConcurrentOrderResults(
                        statuses, initialOrderCount, initialRequestCount);
                assertConcurrentDownstreamRequests();
        } finally {
                mockWebServer.setDispatcher(new QueueDispatcher());
        }
    }

    @Test
    void shouldCreateOrderWithExpressShipping() throws Exception {
        UUID customerId = UUID.randomUUID();
        String token = createToken(customerId);
        String idempotencyKey = UUID.randomUUID().toString();

        OrderRequest request =
                new OrderRequest(
                        List.of(new OrderItemRequest(productId, 2)),
                        shippingAddress(),
                        ShippingMethod.EXPRESS);

        enqueueSuccessfulOrderDependencies(ShippingMethod.EXPRESS, expressRate);
        mockMvc.perform(
                createOrderRequest(token, idempotencyKey, request))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PAID"))
                .andExpect(jsonPath("$.shipping").value(25.00));

        verifySuccessfulDownstreamRequests();
    }

    @Test
    @Timeout(10)
    void shouldReleaseInventoryWhenShippingFails() throws Exception {
        UUID customerId = UUID.randomUUID();
        String token = createToken(customerId);
        String idempotencyKey = UUID.randomUUID().toString();
    
        OrderRequest request =
                new OrderRequest(
                        List.of(new OrderItemRequest(productId, 2)),
                        shippingAddress(),
                        ShippingMethod.STANDARD);
    
        enqueueShippingFailureResponses();
    
        mockMvc.perform(
                createOrderRequest(token, idempotencyKey, request))
                .andExpect(status().isBadGateway());
    
        assertShippingFailureRequests();
    }

    private void enqueueShippingFailureResponses() throws JsonProcessingException {
        // Customer
        mockWebServer.enqueue(
                new MockResponse().setResponseCode(200));
    
        // Inventory reservation
        mockWebServer.enqueue(
                new MockResponse().setResponseCode(200));
    
        // Product
        mockWebServer.enqueue(
                new MockResponse()
                        .setResponseCode(200)
                        .setBody(
                                objectMapper.writeValueAsString(
                                        productSnapshot()))
                        .addHeader("Content-Type", "application/json"));
    
        // Shipping attempts
        mockWebServer.enqueue(new MockResponse().setResponseCode(500));
        mockWebServer.enqueue(new MockResponse().setResponseCode(500));
        mockWebServer.enqueue(new MockResponse().setResponseCode(500));
    
        // Inventory release
        mockWebServer.enqueue(
                new MockResponse().setResponseCode(200));
    }

    private void assertShippingFailureRequests() throws InterruptedException {
        RecordedRequest customer = mockWebServer.takeRequest();
        RecordedRequest reservation = mockWebServer.takeRequest();
        RecordedRequest product = mockWebServer.takeRequest();
        RecordedRequest shippingAttempt1 = mockWebServer.takeRequest();
        RecordedRequest shippingAttempt2 = mockWebServer.takeRequest();
        RecordedRequest shippingAttempt3 = mockWebServer.takeRequest();
        RecordedRequest release = mockWebServer.takeRequest();
    
        assertThat(customer.getMethod()).isEqualTo("HEAD");
        assertThat(customer.getPath()).startsWith("/api/customers/");
    
        assertThat(reservation.getMethod()).isEqualTo("POST");
        assertThat(reservation.getPath()).endsWith("/reserve");
    
        assertThat(product.getMethod()).isEqualTo("GET");
        assertThat(product.getPath()).startsWith("/api/products/");
    
        assertShippingAttempt(shippingAttempt1);
        assertShippingAttempt(shippingAttempt2);
        assertShippingAttempt(shippingAttempt3);
    
        assertThat(release.getMethod()).isEqualTo("POST");
        assertThat(release.getPath()).endsWith("/release");
    }

    private void assertShippingAttempt(RecordedRequest request) {
        assertThat(request.getMethod()).isEqualTo("POST");
        assertThat(request.getPath()).isEqualTo("/api/shipping/quotes");
    }

    private MockHttpServletRequestBuilder createOrderRequest(
        String token,
        String idempotencyKey,
        OrderRequest request) throws JsonProcessingException {

        return post("/api/orders")
                .header(AUTHORIZATION_HEADER, "Bearer " + token)
                .header(IDEMPOTENCY_HEADER, idempotencyKey)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request));
    }

       
    private Dispatcher concurrentOrderDispatcher() {
        return new Dispatcher() {
    
            @Override
            public MockResponse dispatch(RecordedRequest request) {
                String path = request.getPath();
    
                MockResponse response = customerResponse(request, path);
    
                if (response != null) {
                    return response;
                }
    
                response = inventoryResponse(request, path);
    
                if (response != null) {
                    return response;
                }
    
                response = productResponse(request, path);
    
                if (response != null) {
                    return response;
                }
    
                response = shippingResponse(request, path);
    
                if (response != null) {
                    return response;
                }
    
                response = paymentResponse(request, path);
    
                if (response != null) {
                    return response;
                }
    
                return new MockResponse().setResponseCode(404);
            }
        };
    }
    
    private MockResponse customerResponse(
        RecordedRequest request,
        String path) {

        if ("HEAD".equals(request.getMethod())
                && path.startsWith("/api/customers/")) {

                return new MockResponse().setResponseCode(200);
        }

        return null;
    }

    private MockResponse inventoryResponse(
        RecordedRequest request,
        String path) {

        if ("POST".equals(request.getMethod())
                && path.endsWith("/reserve")) {

                return new MockResponse().setResponseCode(200);
        }

        if ("POST".equals(request.getMethod())
                && path.endsWith("/release")) {

                return new MockResponse().setResponseCode(200);
        }

        return null;
    }

    private MockResponse productResponse(
        RecordedRequest request,
        String path) {

        if ("GET".equals(request.getMethod())
                && path.startsWith("/api/products/")) {

                return productResponse();
        }

        return null;
    }

    private MockResponse shippingResponse(
        RecordedRequest request,
        String path) {

        if (!"POST".equals(request.getMethod())
                || !"/api/shipping/quotes".equals(path)) {

                return null;
        }

        try {
                return new MockResponse()
                        .setResponseCode(200)
                        .setHeader("Content-Type", "application/json")
                        .setBody(
                                shippingQuoteResponseJson(
                                        UUID.randomUUID(),
                                        ShippingMethod.STANDARD,
                                        standardRate));
        } catch (JsonProcessingException exception) {
                throw new RuntimeException(exception);
        }
    }

    private MockResponse paymentResponse(
        RecordedRequest request,
        String path) {

        if ("POST".equals(request.getMethod())
                && "/api/payments".equals(path)) {

                return new MockResponse().setResponseCode(201);
        }

        return null;
    }

    private MockResponse productResponse() {
        return new MockResponse()
                    .setResponseCode(200)
                    .setBody(objectMapper.writeValueAsString(productSnapshot()))
                    .addHeader("Content-Type", "application/json");
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
                                .header(AUTHORIZATION_HEADER, "Bearer " + token)
                                .header(IDEMPOTENCY_HEADER, idempotencyKey)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestJson))
                .andReturn()
                .getResponse()
                .getStatus();
        }
    

    private void assertConcurrentDownstreamRequests()
        throws InterruptedException {

        int reserveRequests = 0;

        for (int i = 0; i < SUCCESSFUL_ORDER_DOWNSTREAM_REQUEST_COUNT * 2; i++) {
                RecordedRequest request =
                        mockWebServer.takeRequest(5, TimeUnit.SECONDS);

                assertThat(request).isNotNull();

                if ("POST".equals(request.getMethod())
                        && request.getPath().endsWith("/reserve")) {
                reserveRequests++;
                }
        }

        assertThat(reserveRequests)
                .as("Inventory reserve requests")
                .isEqualTo(2);
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
                .isEqualTo(initialRequestCount + (SUCCESSFUL_ORDER_DOWNSTREAM_REQUEST_COUNT * 2));
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


    private void enqueueSuccessfulOrderDependencies(
        ShippingMethod shippingMethod,
        BigDecimal shippingPrice)
        throws JsonProcessingException {

        // Customer
        mockWebServer.enqueue(
                new MockResponse()
                        .setResponseCode(200)
                        .addHeader("Content-Length", "0"));

        // Inventory
        mockWebServer.enqueue(
                new MockResponse()
                        .setResponseCode(200)
                        .addHeader("Content-Length", "0"));

        // Product
        mockWebServer.enqueue(
                new MockResponse()
                        .setResponseCode(200)
                        .setBody(
                                objectMapper.writeValueAsString(productSnapshot()))
                        .addHeader("Content-Type", "application/json"));

        // Shipping
        mockWebServer.enqueue(
                new MockResponse()
                        .setResponseCode(200)
                        .setBody(
                                shippingQuoteResponseJson(
                                        UUID.randomUUID(),
                                        shippingMethod,
                                        shippingPrice))
                        .addHeader(
                                "Content-Type",
                                "application/json"));

        // Payment
        mockWebServer.enqueue(
                new MockResponse()
                        .setResponseCode(201)
                        .addHeader("Content-Length", "0"));
        }

        private void verifySuccessfulDownstreamRequests() throws InterruptedException {

                RecordedRequest customerRequest =
                        mockWebServer.takeRequest();
            
                RecordedRequest inventoryRequest =
                        mockWebServer.takeRequest();
            
                RecordedRequest productRequest =
                        mockWebServer.takeRequest();
            
                RecordedRequest shippingRequest =
                        mockWebServer.takeRequest();
            
                RecordedRequest paymentRequest =
                        mockWebServer.takeRequest();
            
                assertThat(customerRequest.getMethod()).isEqualTo("HEAD");
                assertThat(customerRequest.getPath()).startsWith("/api/customers/");
              
                assertThat(inventoryRequest.getMethod()).isEqualTo("POST");
                assertThat(inventoryRequest.getPath()).startsWith("/api/inventory/");
                assertThat(inventoryRequest.getPath()).endsWith("/reserve");
               
                assertThat(productRequest.getMethod()).isEqualTo("GET");
                assertThat(productRequest.getPath()).startsWith("/api/products/");
               
                assertThat(shippingRequest.getMethod()).isEqualTo("POST");
                assertThat(shippingRequest.getPath()).isEqualTo("/api/shipping/quotes");
                
                assertThat(paymentRequest.getMethod()).isEqualTo("POST");
                assertThat(paymentRequest.getPath()).isEqualTo("/api/payments");
        }


        private String shippingQuoteResponseJson(
                UUID orderId,
                ShippingMethod shippingMethod,
                BigDecimal price)
                throws JsonProcessingException {
        
            ShippingQuoteResponse response =
                    ShippingQuoteResponse.builder()
                            .id(UUID.randomUUID())
                            .orderId(orderId)
                            .destination(
                                    ShippingAddressResponse.builder()
                                            .addressLine1("123 Test Street")
                                            .city("Auckland")
                                            .postcode("1010")
                                            .country("NZ")
                                            .build())
                            .weightKg(new BigDecimal("0.500"))
                            .shippingMethod(shippingMethod)
                            .price(price)
                            .currency("NZD")
                            .estimatedDeliveryMin(2)
                            .estimatedDeliveryMax(4)
                            .expiresAt(LocalDateTime.now().plusHours(1))
                            .createdAt(LocalDateTime.now())
                            .build();
        
            return objectMapper.writeValueAsString(response);
        }

        private void drainRecordedRequests() throws InterruptedException {
            while (mockWebServer.takeRequest(0, TimeUnit.MILLISECONDS) != null) {
                // Drain requests recorded by the previous test.
            }
        }
}
