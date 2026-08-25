package nz.fox.craig.api.gateway;

import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.exactly;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ApiGatewayRoutingTest {

    private static final WireMockServer PRODUCT_SERVICE = new WireMockServer(0);
    private static final WireMockServer CUSTOMER_SERVICE = new WireMockServer(0);
    private static final WireMockServer ORDER_SERVICE = new WireMockServer(0);
    private static final WireMockServer AUTHENTICATION_SERVICE = new WireMockServer(0);


    @Autowired
    private WebTestClient webTestClient;

    @BeforeAll
    static void startWireMock() {
        PRODUCT_SERVICE.start();
        CUSTOMER_SERVICE.start();
        ORDER_SERVICE.start();
        AUTHENTICATION_SERVICE.start();
    }

    @AfterAll
    static void stopWireMock() {
        PRODUCT_SERVICE.stop();
        CUSTOMER_SERVICE.stop();
        ORDER_SERVICE.stop();
        AUTHENTICATION_SERVICE.stop();
    }

    @BeforeEach
    void resetWireMock() {
        PRODUCT_SERVICE.resetAll();
        CUSTOMER_SERVICE.resetAll();
        ORDER_SERVICE.resetAll();
        AUTHENTICATION_SERVICE.resetAll();
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add(
            "PRODUCT_SERVICE_URL",
            () -> "http://localhost:" + PRODUCT_SERVICE.port()
        );

        registry.add(
            "CUSTOMER_SERVICE_URL",
            () -> "http://localhost:" + CUSTOMER_SERVICE.port()
        );

        registry.add(
            "ORDER_SERVICE_URL",
            () -> "http://localhost:" + ORDER_SERVICE.port()
        );

        registry.add(
            "AUTHENTICATION_SERVICE_URL",
            () -> "http://localhost:" + AUTHENTICATION_SERVICE.port()
        );
    }

    @Test
    void shouldRouteProductRequestToProductService() {
        PRODUCT_SERVICE.stubFor(
            get(urlEqualTo("/api/products"))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                            [
                                {
                                    "id": "123",
                                    "name": "Test Product"
                                }
                            ]
                            """)
                )
        );

        // The actual gateway request/assertion goes here.
        webTestClient
            .get()
            .uri("/api/products")
            .exchange()
            .expectStatus().isOk()
            .expectHeader().contentType("application/json")
            .expectBody()
            .jsonPath("$[0].name").isEqualTo("Test Product");

        PRODUCT_SERVICE.verify(
            getRequestedFor(urlEqualTo("/api/products"))
        );
    }


    @Test
    void shouldRejectProductRequestWhenProductServiceFails() {
    
        PRODUCT_SERVICE.stubFor(
            get(urlEqualTo("/api/products"))
                .willReturn(
                    aResponse()
                        .withStatus(500)
                )
        );
    
        webTestClient
            .get()
            .uri("/api/products")
            .exchange()
            .expectStatus().is5xxServerError();

        PRODUCT_SERVICE.verify(
            exactly(1),
            getRequestedFor(urlEqualTo("/api/products"))
        );
    }

    @Test
    void shouldRetrieveImagesThroughProductService() {
        PRODUCT_SERVICE.stubFor(
            get(urlEqualTo("/images/categories/test.jpg"))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "image/jpeg")
                        .withBody("test image".getBytes())
                )
        );

        webTestClient
            .get()
            .uri("/images/categories/test.jpg")
            .exchange()
            .expectStatus().isOk()
            .expectHeader().contentType("image/jpeg");

        PRODUCT_SERVICE.verify(
            exactly(1),
            getRequestedFor(urlEqualTo("/images/categories/test.jpg"))
        );

    }

    @Test
    void shouldRouteCustomerRequestToCustomerService() {
        CUSTOMER_SERVICE.stubFor(
            post(urlEqualTo("/api/customers"))
                .withHeader("Content-Type", containing("application/json"))
                .withRequestBody(matchingJsonPath("$.firstName", equalTo("John")))
                .withRequestBody(matchingJsonPath("$.lastName", equalTo("Smith")))
                .withRequestBody(matchingJsonPath("$.email", equalTo("john.smith@example.com")))
                .withRequestBody(matchingJsonPath("$.address", equalTo("123 Test Street")))
                .willReturn(
                    aResponse()
                        .withStatus(201)
                        .withHeader("Content-Type", "application/json")
                        .withBody(Payloads.CUSTOMER_REQUEST)
                )
        );

        webTestClient
            .post()
            .uri("/api/customers")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(Payloads.CUSTOMER_RESPONSE)
            .exchange()
            .expectStatus().isCreated()
            .expectHeader().contentType("application/json")
            .expectBody()
            .jsonPath("$.firstName").isEqualTo("John")
            .jsonPath("$.lastName").isEqualTo("Smith")
            .jsonPath("$.displayName").isEqualTo("John Smith")
            .jsonPath("$.email").isEqualTo("john.smith@example.com")
            .jsonPath("$.address").isEqualTo("123 Test Street")
            .jsonPath("$.status").isEqualTo("ACTIVE");

        CUSTOMER_SERVICE.verify(
            exactly(1),
            postRequestedFor(urlEqualTo("/api/customers"))
        );

    }

    @Test
    void shouldRejectCustomerRequestWhenCustomerServiceFails() {
        CUSTOMER_SERVICE.stubFor(
            post(urlEqualTo("/api/customers"))
                .willReturn(
                    aResponse()
                        .withStatus(500)
                )
        );

        webTestClient
            .post()
            .uri("/api/customers")
            .exchange()
            .expectStatus().is5xxServerError();

        CUSTOMER_SERVICE.verify(
            exactly(1),
            postRequestedFor(urlEqualTo("/api/customers"))
        );

    }

   
    @Test
    void shouldCreateOrderThroughOrderService() {
        ORDER_SERVICE.stubFor(
            post(urlEqualTo("/api/orders"))
                .withHeader("Idempotency-Key", equalTo("test-idempotency-key"))
                .withHeader("Content-Type", containing("application/json"))
                .withRequestBody(matchingJsonPath(
                    "$.customerId",
                    equalTo("550e8400-e29b-41d4-a716-446655440000")))
                .willReturn(
                    aResponse()
                        .withStatus(201)
                        .withHeader("Content-Type", "application/json")
                        .withBody(Payloads.ORDER_RESPONSE))
        );

        webTestClient
            .post()
            .uri("/api/orders")
            .contentType(MediaType.APPLICATION_JSON)
            .header("Idempotency-Key", "test-idempotency-key")
            .bodyValue(Payloads.ORDER_REQUEST)
            .exchange()
            .expectStatus().isCreated()
            .expectHeader().contentType("application/json")
            .expectBody()
            .jsonPath("$.status").isEqualTo("PLACED")
            .jsonPath("$.items[0].productName").isEqualTo("Test Product");

        ORDER_SERVICE.verify(
            exactly(1),
            postRequestedFor(urlEqualTo("/api/orders"))
                .withHeader("Idempotency-Key", equalTo("test-idempotency-key"))
        );
    }
      

     

    @Test
    void shouldRetrieveAuthenticatedCustomerOrdersThroughOrderService() {
        ORDER_SERVICE.stubFor(
            get(urlEqualTo("/api/orders"))
                .withHeader("Authorization", equalTo("Bearer test-jwt"))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(Payloads.ORDERS_RETURNED)
                )
        );

        webTestClient
            .get()
            .uri("/api/orders")
            .header("Authorization", "Bearer test-jwt")
            .exchange()
            .expectStatus().isOk()
            .expectHeader().contentType("application/json")
            .expectBody()
            .jsonPath("$[0].status").isEqualTo("PLACED")
            .jsonPath("$[0].items[0].productName").isEqualTo("Test Product");

        ORDER_SERVICE.verify(
            exactly(1),
            getRequestedFor(urlEqualTo("/api/orders"))
                .withHeader("Authorization", equalTo("Bearer test-jwt"))
        );
    }

    @Test
    void shouldReturnServerErrorWhenCreateOrderFails() {
    
        ORDER_SERVICE.stubFor(
            post(urlEqualTo("/api/orders"))
                .willReturn(
                    aResponse()
                        .withStatus(500)
                )
        );
    
        webTestClient
            .post()
            .uri("/api/orders")
            .contentType(MediaType.APPLICATION_JSON)
            .header("Idempotency-Key", "test-idempotency-key")
            .bodyValue("""
                {
                    "customerId": "550e8400-e29b-41d4-a716-446655440000",
                    "items": [
                        {
                            "productId": "750e8400-e29b-41d4-a716-446655440000",
                            "quantity": 1
                        }
                    ]
                }
                """)
            .exchange()
            .expectStatus().is5xxServerError();
    
        ORDER_SERVICE.verify(
            exactly(1),
            postRequestedFor(urlEqualTo("/api/orders"))
        );
    }

    @Test
    void shouldReturnServerErrorWhenGetOrdersFails() {

        ORDER_SERVICE.stubFor(
            get(urlEqualTo("/api/orders"))
                .willReturn(
                    aResponse()
                        .withStatus(500)
                )
        );

        webTestClient
            .get()
            .uri("/api/orders")
            .header("Authorization", "Bearer test-jwt")
            .exchange()
            .expectStatus().is5xxServerError();

        ORDER_SERVICE.verify(
            exactly(1),
            getRequestedFor(urlEqualTo("/api/orders"))
        );
    }

    @Test
    void shouldRouteLoginRequestToAuthenticationService() {

        AUTHENTICATION_SERVICE.stubFor(
            post(urlEqualTo("/api/auth/login"))
                .withHeader("Content-Type", containing("application/json"))
                .withRequestBody(
                    matchingJsonPath("$.email", equalTo("test@example.com"))
                )
                .withRequestBody(
                    matchingJsonPath("$.password", equalTo("password"))
                )
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                            {
                                "token": "test-jwt",
                                "customerId": "550e8400-e29b-41d4-a716-446655440000",
                                "firstName": "Test",
                                "preferredName": null
                            }
                            """)
                )
        );

        webTestClient
            .post()
            .uri("/api/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue("""
                {
                    "email": "test@example.com",
                    "password": "password"
                }
                """)
            .exchange()
            .expectStatus().isOk()
            .expectHeader().contentType("application/json")
            .expectBody()
            .jsonPath("$.token").isEqualTo("test-jwt")
            .jsonPath("$.customerId")
                .isEqualTo("550e8400-e29b-41d4-a716-446655440000")
            .jsonPath("$.firstName").isEqualTo("Test")
            .jsonPath("$.preferredName").isEmpty();

        AUTHENTICATION_SERVICE.verify(
            exactly(1),
            postRequestedFor(urlEqualTo("/api/auth/login"))
        );
    }

    @Test
    void shouldReturnServerErrorWhenAuthenticationServiceFails() {

        AUTHENTICATION_SERVICE.stubFor(
            post(urlEqualTo("/api/auth/login"))
                .willReturn(
                    aResponse()
                        .withStatus(500)
                )
        );

        webTestClient
            .post()
            .uri("/api/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue("""
                {
                    "email": "test@example.com",
                    "password": "password"
                }
                """)
            .exchange()
            .expectStatus().is5xxServerError();

        AUTHENTICATION_SERVICE.verify(
            exactly(1),
            postRequestedFor(urlEqualTo("/api/auth/login"))
        );
    }
}
