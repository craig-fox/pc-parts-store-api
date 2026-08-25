package nz.fox.craig.order.client;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.net.http.HttpClient;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import nz.fox.craig.order.config.HttpClientProperties;
import nz.fox.craig.order.config.ProductServiceProperties;
import nz.fox.craig.order.config.RestClientConfig;
import nz.fox.craig.order.exception.DownstreamServiceUnavailableException;
import nz.fox.craig.security.JwtPropagationInterceptor;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.web.client.RestClientAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Import(RestClientConfig.class)
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
    
        for (int i = 0; i < 3; i++) {
            mockWebServer.enqueue(
                    new MockResponse()
                            .setHeadersDelay(500, TimeUnit.MILLISECONDS)
                            .setHeader("Content-Type", "application/json")
            );
        }
    
        contextRunner
        .withPropertyValues(
                "services.product.base-url=" + mockWebServer.url("/"),
                "services.http-client.read-timeout=100ms",
                "services.http-client.connect-timeout=100ms"
            )
                .run(context -> {
    
                    HttpProductClient productClient =
                            context.getBean(HttpProductClient.class);
    
                    assertThatThrownBy(
                            () -> productClient.getProduct(UUID.randomUUID())
                    )
                            .isInstanceOf(DownstreamServiceUnavailableException.class)
                            .hasMessage("Product service is unavailable");
                });
    }

    @Configuration
    @EnableConfigurationProperties(HttpClientProperties.class)
    static class TestConfig {
    
        @Bean
        ProductServiceProperties productServiceProperties(
                Environment environment) {
    
            return new ProductServiceProperties(
                    environment.getProperty("services.product.base-url"));
        }
    
        @Bean
        JwtPropagationInterceptor jwtPropagationInterceptor() {
            return new JwtPropagationInterceptor();
        }
    
        @Bean
        ClientHttpRequestFactory downstreamRequestFactory(
                HttpClientProperties properties) {
    
            HttpClient httpClient = HttpClient.newBuilder()
                    .connectTimeout(properties.connectTimeout())
                    .build();
    
            JdkClientHttpRequestFactory factory =
                    new JdkClientHttpRequestFactory(httpClient);
    
            factory.setReadTimeout(properties.readTimeout());
    
            return factory;
        }
    
        @Bean(name = "productRestClient")
        RestClient productRestClient(
                RestClient.Builder builder,
                ProductServiceProperties properties,
                JwtPropagationInterceptor interceptor,
                ClientHttpRequestFactory requestFactory) {
    
            return builder
                    .baseUrl(properties.baseUrl())
                    .requestInterceptor(interceptor)
                    .requestFactory(requestFactory)
                    .build();
        }
    
        @Bean
        HttpProductClient httpProductClient(
                @Qualifier("productRestClient") RestClient restClient) {
    
            return new HttpProductClient(restClient);
        }
    }
}