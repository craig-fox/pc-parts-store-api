package nz.fox.craig.order.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {
    @Bean(name = "customerRestClient")
    RestClient customerRestClient(
            RestClient.Builder builder,
            CustomerServiceProperties properties) {

        return builder
                .baseUrl(properties.baseUrl())
                .build();
    }

    @Bean(name = "productRestClient")
    RestClient productRestClient(
            RestClient.Builder builder,
            ProductServiceProperties properties) {

        return builder
                .baseUrl(properties.baseUrl())
                .build();
    }
}
