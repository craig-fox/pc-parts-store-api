package nz.fox.craig.order.client;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;

import nz.fox.craig.order.config.ProductServiceProperties;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.web.client.RestClientAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

class HttpProductClientTimeoutTest {

    private static MockWebServer mockWebServer;

    private final ApplicationContextRunner contextRunner =
            new ApplicationContextRunner()
                    .withConfiguration(
                            AutoConfigurations.of(RestClientAutoConfiguration.class)
                    )
                    .withUserConfiguration(TestConfig.class)
                    .withPropertyValues(
                            "services.product.base-url=http://localhost",
                            "spring.http.client.read-timeout=200ms"
                    );

    @BeforeAll
    static void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();
    }

    @AfterAll
    static void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    @Test
    void getProduct_timesOutWhenProductServiceDoesNotRespond() {
        mockWebServer.enqueue(
                new MockResponse()
                        .setBody("{}")
                        .setHeadersDelay(1, TimeUnit.SECONDS)
        );

        contextRunner
                .withPropertyValues(
                        "services.product.base-url=" + mockWebServer.url("/")
                )
                .run(context -> {
                    HttpProductClient productClient =
                            context.getBean(HttpProductClient.class);

                    assertThatThrownBy(
                            () -> productClient.getProduct(UUID.randomUUID())
                    ).isInstanceOf(RestClientException.class);
                });
    }

    @Configuration
    static class TestConfig {

        @Bean
        ProductServiceProperties productServiceProperties(
                org.springframework.core.env.Environment environment) {

            return new ProductServiceProperties(
                    environment.getProperty("services.product.base-url")
            );
        }

        @Bean(name = "productRestClient")
        RestClient productRestClient(
                RestClient.Builder builder,
                ProductServiceProperties properties) {

            return builder
                    .baseUrl(properties.baseUrl())
                    .build();
        }

        @Bean
        HttpProductClient httpProductClient(
                @Qualifier("productRestClient") RestClient restClient) {

            return new HttpProductClient(restClient);
        }
    }
}