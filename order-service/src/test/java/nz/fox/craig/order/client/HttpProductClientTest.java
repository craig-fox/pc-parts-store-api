package nz.fox.craig.order.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.util.UUID;

import nz.fox.craig.order.dto.client.ProductSnapshot;
import nz.fox.craig.order.exception.DownstreamServiceUnavailableException;
import nz.fox.craig.order.exception.ProductNotFoundException;
import nz.fox.craig.order.utils.SampleResponses;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.web.client.RestClient;

class HttpProductClientTest {

    private MockWebServer server;

    private ProductClient productClient;

    @BeforeEach
    void setUp() throws IOException {

        server = new MockWebServer();
        server.start();

        RestClient restClient =
                RestClient.builder()
                        .baseUrl(server.url("/").toString())
                        .build();

        productClient = new HttpProductClient(restClient);
    }

    @AfterEach
    void tearDown() throws IOException {
        server.shutdown();
    }

    @Test
    void shouldReturnProduct() throws Exception {

        UUID productId = UUID.randomUUID();

        server.enqueue(
                new MockResponse()
                        .setHeader("Content-Type", "application/json")
                        .setBody(
                                SampleResponses
                                        .gamingMouse(productId)
                                        .formatted(productId)));

        ProductSnapshot product =
                productClient.getProduct(productId);

        assertThat(product.id()).isEqualTo(productId);
        assertThat(product.name()).isEqualTo("Gaming Mouse");

        RecordedRequest request = server.takeRequest();

        assertThat(request.getMethod()).isEqualTo("GET");
        assertThat(request.getPath())
                .isEqualTo("/api/products/" + productId);
    }

    @Test
    void shouldThrowProductNotFoundException()
            throws InterruptedException {

        UUID productId = UUID.randomUUID();

        server.enqueue(
                new MockResponse()
                        .setResponseCode(404));

        assertThatThrownBy(
                () -> productClient.getProduct(productId))
                .isInstanceOf(ProductNotFoundException.class)
                .hasMessageContaining(productId.toString());

        RecordedRequest request =
                server.takeRequest();

        assertThat(request.getMethod()).isEqualTo("GET");
        assertThat(request.getPath())
                .isEqualTo("/api/products/" + productId);
    }

    @Test
    void shouldThrowDownstreamServiceUnavailableWhenServerReturns500()
            throws InterruptedException {

        UUID productId = UUID.randomUUID();

        server.enqueue(
                new MockResponse()
                        .setResponseCode(500));

        assertThatThrownBy(
                () -> productClient.getProduct(productId))
                .isInstanceOf(DownstreamServiceUnavailableException.class)
                .hasMessage("Product service is unavailable");

        RecordedRequest request =
                server.takeRequest();

        assertThat(request.getMethod()).isEqualTo("GET");
        assertThat(request.getPath())
                .isEqualTo("/api/products/" + productId);
    }

    @Test
    void shouldThrowDownstreamServiceUnavailableWhenProductServiceCannotBeReached()
            throws Exception {

        UUID productId = UUID.randomUUID();

        server.shutdown();

        assertThatThrownBy(
                () -> productClient.getProduct(productId))
                .isInstanceOf(DownstreamServiceUnavailableException.class)
                .hasMessage("Product service is unavailable");
    }

}