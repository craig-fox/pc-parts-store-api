package nz.fox.craig.order.client.retry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.util.UUID;

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
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.client.RestClient;

import nz.fox.craig.order.client.CustomerClient;
import nz.fox.craig.order.client.HttpCustomerClient;
import nz.fox.craig.order.config.CustomerServiceProperties;
import nz.fox.craig.order.exception.CustomerNotFoundException;
import nz.fox.craig.order.exception.DownstreamServiceUnavailableException;
import nz.fox.craig.security.JwtPropagationInterceptor;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;

@SpringBootTest(
    classes = HttpCustomerClientRetryTest.TestConfiguration.class)
class HttpCustomerClientRetryTest {

    private static MockWebServer server;

    @Autowired
    private CustomerClient customerClient;

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
                "services.customer.base-url",
                () -> server.url("/").toString());
    }


    @Test
    void shouldRetryWhenCustomerServiceReturnsServerError() {
    
        UUID customerId = UUID.randomUUID();
    
        server.enqueue(new MockResponse().setResponseCode(500));
        server.enqueue(new MockResponse().setResponseCode(500));
        server.enqueue(new MockResponse().setResponseCode(200));
    
        long requestsBefore = server.getRequestCount();
    
        customerClient.validateCustomerExists(customerId);
    
        long requestsAfter = server.getRequestCount();
    
        assertThat(requestsAfter - requestsBefore).isEqualTo(3);
    }

    @Test
    void shouldStopRetryingAfterMaximumAttempts() throws Exception {
    
        UUID customerId = UUID.randomUUID();
    
        server.enqueue(new MockResponse().setResponseCode(500));
        server.enqueue(new MockResponse().setResponseCode(500));
        server.enqueue(new MockResponse().setResponseCode(500));
    
        assertThatThrownBy(
                () -> customerClient.validateCustomerExists(customerId))
                .isInstanceOf(DownstreamServiceUnavailableException.class);
    
        server.takeRequest();
        server.takeRequest();
        server.takeRequest();
    }

    @Test
    void shouldNotRetryWhenCustomerDoesNotExist() {
    
        UUID customerId = UUID.randomUUID();
    
        server.enqueue(new MockResponse().setResponseCode(404));
    
        long requestsBefore = server.getRequestCount();
    
        assertThatThrownBy(
                () -> customerClient.validateCustomerExists(customerId))
                .isInstanceOf(CustomerNotFoundException.class)
                .hasMessageContaining(customerId.toString());
    
        long requestsAfter = server.getRequestCount();
    
        assertThat(requestsAfter - requestsBefore).isEqualTo(1);
    }


    @Configuration
    @EnableAutoConfiguration(exclude = {
            DataSourceAutoConfiguration.class,
            FlywayAutoConfiguration.class,
            HibernateJpaAutoConfiguration.class
    })
    @EnableConfigurationProperties(CustomerServiceProperties.class)
    static class TestConfiguration {

        @Bean
        JwtPropagationInterceptor jwtPropagationInterceptor() {
            return new JwtPropagationInterceptor();
        }

        @Bean(name = "customerRestClient")
        RestClient customerRestClient(
                RestClient.Builder builder,
                CustomerServiceProperties properties,
                JwtPropagationInterceptor interceptor) {

            return builder
                    .baseUrl(properties.baseUrl())
                    .requestInterceptor(interceptor)
                    .build();
        }

        @Bean
        CustomerClient customerClient(
                @Qualifier("customerRestClient") RestClient restClient) {

            return new HttpCustomerClient(restClient);
        }
    }

}
