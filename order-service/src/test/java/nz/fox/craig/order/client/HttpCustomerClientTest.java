package nz.fox.craig.order.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.util.UUID;

import nz.fox.craig.order.exception.CustomerNotFoundException;
import nz.fox.craig.order.exception.DownstreamServiceUnavailableException;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.web.client.RestClient;

class HttpCustomerClientTest {

    private MockWebServer server;

    private CustomerClient customerClient;

    @BeforeEach
    void setUp() throws IOException {

        server = new MockWebServer();
        server.start();

        RestClient restClient =
                RestClient.builder()
                        .baseUrl(server.url("/").toString())
                        .build();

        customerClient = new HttpCustomerClient(restClient);
    }

    @AfterEach
    void tearDown() throws IOException {
        server.shutdown();
    }

    @Test
    void shouldValidateCustomerExists() throws Exception {

        UUID customerId = UUID.randomUUID();

        server.enqueue(
                new MockResponse()
                        .setResponseCode(200));

        customerClient.validateCustomerExists(customerId);

        RecordedRequest request = server.takeRequest();

        assertThat(request.getMethod()).isEqualTo("HEAD");
        assertThat(request.getPath())
                .isEqualTo("/api/customers/" + customerId);
    }

    @Test
    void shouldThrowCustomerNotFoundException()
            throws InterruptedException {

        UUID customerId = UUID.randomUUID();

        server.enqueue(
                new MockResponse()
                        .setResponseCode(404));

        assertThatThrownBy(
                () -> customerClient.validateCustomerExists(customerId))
                .isInstanceOf(CustomerNotFoundException.class)
                .hasMessageContaining(customerId.toString());

        RecordedRequest request = server.takeRequest();

        assertThat(request.getMethod()).isEqualTo("HEAD");
        assertThat(request.getPath())
                .isEqualTo("/api/customers/" + customerId);
    }

    @Test
    void shouldThrowDownstreamServiceUnavailableWhenServerReturns500()
            throws InterruptedException {

        UUID customerId = UUID.randomUUID();

        server.enqueue(
                new MockResponse()
                        .setResponseCode(500));

        assertThatThrownBy(
                () -> customerClient.validateCustomerExists(customerId))
                .isInstanceOf(DownstreamServiceUnavailableException.class)
                .hasMessage("Customer service is unavailable");

        RecordedRequest request = server.takeRequest();

        assertThat(request.getMethod()).isEqualTo("HEAD");
        assertThat(request.getPath())
                .isEqualTo("/api/customers/" + customerId);
    }

    @Test
    void shouldThrowDownstreamServiceUnavailableWhenCustomerServiceCannotBeReached()
            throws Exception {

        UUID customerId = UUID.randomUUID();

        server.shutdown();

        assertThatThrownBy(
                () -> customerClient.validateCustomerExists(customerId))
                .isInstanceOf(DownstreamServiceUnavailableException.class)
                .hasMessage("Customer service is unavailable");
    }

    
}
