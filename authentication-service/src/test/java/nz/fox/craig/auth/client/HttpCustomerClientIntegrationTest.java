package nz.fox.craig.auth.client;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import nz.fox.craig.auth.dto.AuthenticatedCustomer;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

class HttpCustomerClientIntegrationTest {

    private static MockWebServer mockWebServer;

    private HttpCustomerClient customerClient;

    @BeforeAll
    static void startServer() throws Exception {
        mockWebServer = new MockWebServer();
        mockWebServer.start();
    }

    @AfterAll
    static void stopServer() throws Exception {
        mockWebServer.shutdown();
    }

    @BeforeEach
    void setUp() {
        RestClient restClient =
                RestClient.builder()
                        .baseUrl(mockWebServer.url("/").toString())
                        .build();

        customerClient = new HttpCustomerClient(restClient);
    }

    @Test
    void shouldFindCustomerByEmail() throws Exception {

        UUID customerId = UUID.randomUUID();

        String responseBody =
                """
                {
                  "id": "%s",
                  "email": "jane@example.com",
                  "password": "encoded-password",
                  "active": true,
                  "firstName": "Jane",
                  "preferredName": "Jo"
                }
                """
                        .formatted(customerId);

        mockWebServer.enqueue(
                new MockResponse()
                        .setResponseCode(200)
                        .setBody(responseBody)
                        .addHeader("Content-Type", "application/json"));

        AuthenticatedCustomer customer =
                customerClient.findByEmail("jane@example.com");

        assertThat(customer.id()).isEqualTo(customerId);
        assertThat(customer.email()).isEqualTo("jane@example.com");
        assertThat(customer.password()).isEqualTo("encoded-password");
        assertThat(customer.active()).isTrue();
        assertThat(customer.firstName()).isEqualTo("Jane");
        assertThat(customer.preferredName()).isEqualTo("Jo");

        RecordedRequest request = mockWebServer.takeRequest();

        assertThat(request.getMethod()).isEqualTo("GET");
        assertThat(request.getPath())
            .isEqualTo("/api/customers/email/jane%40example.com");
    }
}
