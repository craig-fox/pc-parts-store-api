package nz.fox.craig.order.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

import nz.fox.craig.security.JwtPropagationInterceptor;

@Configuration
public class RestClientConfig {

    @Bean
    JwtPropagationInterceptor jwtPropagationInterceptor() {
        return new JwtPropagationInterceptor();
    }

    @Bean(name = "customerRestClient")
    RestClient customerRestClient(
            RestClient.Builder builder,
            CustomerServiceProperties properties,
            JwtPropagationInterceptor jwtPropagationInterceptor) {

        return builder
                .baseUrl(properties.baseUrl())
                .requestInterceptor(jwtPropagationInterceptor)
                .build();
    }

    @Bean(name = "productRestClient")
    RestClient productRestClient(
            RestClient.Builder builder,
            ProductServiceProperties properties,
            JwtPropagationInterceptor jwtPropagationInterceptor) {

        return builder
                .baseUrl(properties.baseUrl())
                .requestInterceptor(jwtPropagationInterceptor)
                .build();
    }

    @Bean(name = "inventoryRestClient")
    RestClient inventoryRestClient(
            RestClient.Builder builder,
            InventoryServiceProperties properties,
            JwtPropagationInterceptor jwtPropagationInterceptor) {

        return builder
                .baseUrl(properties.baseUrl())
                .requestInterceptor(jwtPropagationInterceptor)
                .build();
    }
}
