package nz.fox.craig.order.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import nz.fox.craig.order.dto.request.ShippingQuoteRequest;
import nz.fox.craig.order.dto.response.ShippingQuoteResponse;
import nz.fox.craig.order.exception.DownstreamServiceUnavailableException;
import nz.fox.craig.order.fixture.ShippingFixture;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;

class HttpShippingClientTest {
    private MockWebServer mockWebServer;

    private HttpShippingClient shippingClient;

    @BeforeEach
    void setUp() throws Exception {
        mockWebServer = new MockWebServer();
        mockWebServer.start();

        RestClient restClient = RestClient.builder()
                .baseUrl(mockWebServer.url("/").toString())
                .build();

        shippingClient = new HttpShippingClient(restClient);
    }

    @AfterEach
    void tearDown() throws Exception {
        mockWebServer.shutdown();
    }

    @Test
    void shouldReturnShippingQuote() {

        mockWebServer.enqueue(
                new MockResponse()
                        .setResponseCode(201)
                        .setHeader("Content-Type", "application/json")
                        .setBody("""
                                {
                                "id": "%s",
                                "orderId": "%s",
                                "destination": {
                                        "addressLine1": "123 Test Street",
                                        "city": "Auckland",
                                        "postcode": "1010",
                                        "country": "NZ"
                                },
                                "weightKg": 1.5,
                                "shippingMethod": "STANDARD",
                                "price": 15.00,
                                "currency": "NZD",
                                "estimatedDeliveryMin": 3,
                                "estimatedDeliveryMax": 5,
                                "expiresAt": "2026-08-24T18:00:00",
                                "createdAt": "2026-08-24T17:00:00"
                                }
                                """.formatted(
                                        UUID.randomUUID(),
                                        UUID.randomUUID()
                                ))
        );


        ShippingQuoteRequest quoteRequest = ShippingFixture.shippingQuoteRequest();

        ShippingQuoteResponse response =
                shippingClient.calculateQuote(quoteRequest);

        assertThat(response).isNotNull();
        assertThat(response.price()).isEqualByComparingTo("15.00");
        assertThat(response.currency()).isEqualTo("NZD");
    }

    @Test
    void shouldThrowDownstreamServiceUnavailableWhenShippingReturnsServerError() {

        mockWebServer.enqueue(
                new MockResponse()
                        .setResponseCode(503)
        );

        ShippingQuoteRequest request = ShippingFixture.shippingQuoteRequest();

        assertThatThrownBy(() -> shippingClient.calculateQuote(request))
                .isInstanceOf(DownstreamServiceUnavailableException.class)
                .hasMessage("Shipping service is unavailable");
    }

    @Test
    void shouldNotTreatClientErrorAsShippingServiceUnavailable() {

        mockWebServer.enqueue(
                new MockResponse()
                        .setResponseCode(400)
        );

        ShippingQuoteRequest request = ShippingFixture.shippingQuoteRequest();

        assertThatThrownBy(() -> shippingClient.calculateQuote(request))
                .isInstanceOf(HttpClientErrorException.BadRequest.class)
                .isNotInstanceOf(DownstreamServiceUnavailableException.class);
    }
}
