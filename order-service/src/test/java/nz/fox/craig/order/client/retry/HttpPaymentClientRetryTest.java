package nz.fox.craig.order.client.retry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.UUID;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import nz.fox.craig.order.client.HttpPaymentClient;
import nz.fox.craig.order.client.PaymentClient;
import nz.fox.craig.order.config.PaymentServiceProperties;
import nz.fox.craig.order.exception.DownstreamServiceUnavailableException;
import nz.fox.craig.security.JwtPropagationInterceptor;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;

@SpringBootTest(
    classes = HttpPaymentClientRetryTest.TestConfiguration.class
)
public class HttpPaymentClientRetryTest {

    private static MockWebServer server;

    @Autowired
    private PaymentClient paymentClient;

    @BeforeAll
    static void setUp() throws IOException {
        server = new MockWebServer();
        server.start();
    }

    @AfterAll
    static void tearDown() throws IOException {
        server.shutdown();
    }


    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {

        registry.add(
                "services.payment.base-url",
                () -> server.url("/").toString());
    }

    @Test
    void shouldProcessPaymentAfterRetries() {
        server.enqueue(new MockResponse().setResponseCode(500));
        server.enqueue(new MockResponse().setResponseCode(500));
        server.enqueue(new MockResponse().setResponseCode(201));

        long requestsBefore = server.getRequestCount();
        paymentClient.processPayment(UUID.randomUUID(), UUID.randomUUID(), BigDecimal.valueOf(100.00), "NZD");      
        long requestsAfter = server.getRequestCount();
                
        assertThat(requestsAfter - requestsBefore).isEqualTo(3);

    }

    @Test
    void shouldThrowDownstreamServiceIsUnavailableAfterMaxRetries() throws InterruptedException {
        server.enqueue(new MockResponse().setResponseCode(500));
        server.enqueue(new MockResponse().setResponseCode(500));
        server.enqueue(new MockResponse().setResponseCode(500));

        assertThatThrownBy(() -> paymentClient.processPayment(UUID.randomUUID(), UUID.randomUUID(), BigDecimal.valueOf(100.00), "NZD"))
                .isInstanceOf(DownstreamServiceUnavailableException.class);
    
        server.takeRequest();
        server.takeRequest();
        server.takeRequest();
    }

   

    @ParameterizedTest
    @ValueSource(ints = {400, 401, 403, 404, 409, 422, 429})
    void shouldRejectPaymentAfter4xxError(int statusCode) {
        server.enqueue(new MockResponse().setResponseCode(statusCode));
        long requestsBefore = server.getRequestCount();
        assertThatThrownBy(() ->
        paymentClient.processPayment(
                UUID.randomUUID(),
                UUID.randomUUID(),
                BigDecimal.valueOf(100.00),
                "NZD"))
        .isInstanceOf(HttpClientErrorException.class);
        long requestsAfter = server.getRequestCount();
        assertThat(requestsAfter - requestsBefore).isEqualTo(1);
    }

    @Configuration
    @EnableAutoConfiguration(exclude = {
            DataSourceAutoConfiguration.class,
            FlywayAutoConfiguration.class,
            HibernateJpaAutoConfiguration.class
    })
    @EnableConfigurationProperties(PaymentServiceProperties.class)
    static class TestConfiguration {

        @Bean
        JwtPropagationInterceptor jwtPropagationInterceptor() {
            return new JwtPropagationInterceptor();
        }

        @Bean(name = "paymentRestClient")
        RestClient paymentRestClient(
                RestClient.Builder builder,
                PaymentServiceProperties properties,
                JwtPropagationInterceptor interceptor) {

            return builder
                    .baseUrl(properties.baseUrl())
                    .requestInterceptor(interceptor)
                    .build();
        }

        @Bean
        PaymentClient paymentClient(
                @Qualifier("paymentRestClient") RestClient restClient) {

            return new HttpPaymentClient(restClient);
        }
    }

}
