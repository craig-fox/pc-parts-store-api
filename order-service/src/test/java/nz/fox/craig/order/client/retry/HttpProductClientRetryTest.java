package nz.fox.craig.order.client.retry;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.UUID;

import nz.fox.craig.order.client.HttpProductClient;
import nz.fox.craig.order.client.ProductClient;
import nz.fox.craig.order.config.ProductServiceProperties;
import nz.fox.craig.security.JwtPropagationInterceptor;
import nz.fox.craig.order.utils.SampleResponses;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.client.RestClient;

@SpringBootTest(
        classes = HttpProductClientRetryTest.TestConfiguration.class)
class HttpProductClientRetryTest {

    private static MockWebServer server;

    @Autowired
    private ProductClient productClient;

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
                "services.product.base-url",
                () -> server.url("/").toString());
    }

    @Test
    void shouldRetryWhenProductServiceReturnsServerError()
            throws Exception {

        UUID productId = UUID.randomUUID();

        server.enqueue(
                new MockResponse()
                        .setResponseCode(500));

        server.enqueue(
                new MockResponse()
                        .setHeader("Content-Type", "application/json")
                        .setBody(
                                SampleResponses
                                        .gamingMouse(productId)
                                        .formatted(productId)));

        var product = productClient.getProduct(productId);

        assertThat(product.id()).isEqualTo(productId);
        assertThat(server.getRequestCount()).isEqualTo(2);
    }

    @Configuration
    @EnableAutoConfiguration(exclude = {
            DataSourceAutoConfiguration.class,
            FlywayAutoConfiguration.class,
            HibernateJpaAutoConfiguration.class
    })
    @EnableConfigurationProperties(ProductServiceProperties.class)
    static class TestConfiguration {

        @Bean
        JwtPropagationInterceptor jwtPropagationInterceptor() {
            return new JwtPropagationInterceptor();
        }

        @Bean(name = "productRestClient")
        RestClient productRestClient(
                RestClient.Builder builder,
                ProductServiceProperties properties,
                JwtPropagationInterceptor interceptor) {

            return builder
                    .baseUrl(properties.baseUrl())
                    .requestInterceptor(interceptor)
                    .build();
        }

        @Bean
        ProductClient productClient(
                @Qualifier("productRestClient") RestClient restClient) {

            return new HttpProductClient(restClient);
        }
    }
}