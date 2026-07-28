package nz.fox.craig.order.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(CustomerServiceProperties.class)
public class RestClientConfig {
    @Bean(name="customerRestClient")
    RestClient customerRestClient(
            RestClient.Builder builder,
            CustomerServiceProperties properties) {

        return builder
                .baseUrl(properties.baseUrl())
                .build();
    }

    @Bean(name="productRestClient")
    RestClient productRestClient(
            RestClient.Builder builder,
            @Value("${services.product.base-url}") String baseUrl) {

        return builder
                .baseUrl(baseUrl)
                .build();
    }
}
