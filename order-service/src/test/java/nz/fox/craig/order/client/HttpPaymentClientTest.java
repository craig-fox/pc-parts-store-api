package nz.fox.craig.order.client;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import nz.fox.craig.order.exception.DownstreamServiceUnavailableException;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

class HttpPaymentClientTest {
    private MockWebServer mockWebServer;

    private HttpPaymentClient paymentClient;

    @BeforeEach
    void setUp() throws Exception {
        mockWebServer = new MockWebServer();
        mockWebServer.start();

        RestClient restClient = RestClient.builder()
                .baseUrl(mockWebServer.url("/").toString())
                .build();

        paymentClient = new HttpPaymentClient(restClient);
    }

    @AfterEach
    void tearDown() throws Exception {
        mockWebServer.shutdown();
    }

    @Test
    void shouldProcessPayment() throws InterruptedException {

        mockWebServer.enqueue(
                new MockResponse()
                        .setResponseCode(201)
        );

        paymentClient.processPayment(
                UUID.randomUUID(),
                UUID.randomUUID(),
                new BigDecimal("149.99"),
                "NZD"
        );

        RecordedRequest request = mockWebServer.takeRequest();

        assertThat(request.getMethod()).isEqualTo("POST");
        assertThat(request.getPath()).isEqualTo("/api/payments");
    }


    @Test
    void shouldThrowDownstreamServiceUnavailableWhenPaymentReturnsServerError() {

        mockWebServer.enqueue(
                new MockResponse()
                        .setResponseCode(503)
        );

        assertThatThrownBy(() ->
                paymentClient.processPayment(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        new BigDecimal("149.99"),
                        "NZD"))
                .isInstanceOf(DownstreamServiceUnavailableException.class)
                .hasMessage("Payment service is unavailable");
    }

    @Test
    void shouldNotTreatClientErrorAsPaymentServiceUnavailable() {

        mockWebServer.enqueue(
                new MockResponse()
                        .setResponseCode(400)
        );

        assertThatThrownBy(() ->
                paymentClient.processPayment(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        new BigDecimal("149.99"),
                        "NZD"))
                .isInstanceOf(HttpClientErrorException.BadRequest.class)
                .isNotInstanceOf(DownstreamServiceUnavailableException.class);
    }

    @Test
    void shouldThrowDownstreamServiceIsUnavailableAfterResourceAccessException() throws IOException {
        mockWebServer.shutdown();
        assertThatThrownBy(() -> paymentClient.processPayment(UUID.randomUUID(), UUID.randomUUID(), BigDecimal.valueOf(100.00), "NZD"))
                .isInstanceOf(DownstreamServiceUnavailableException.class);
    }

   
}
