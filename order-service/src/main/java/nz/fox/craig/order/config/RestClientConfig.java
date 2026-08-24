package nz.fox.craig.order.config;

import nz.fox.craig.security.JwtPropagationInterceptor;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

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

        return builder.baseUrl(properties.baseUrl())
                .requestInterceptor(jwtPropagationInterceptor)
                .build();
    }

    @Bean(name = "productRestClient")
    RestClient productRestClient(
            RestClient.Builder builder,
            ProductServiceProperties properties,
            JwtPropagationInterceptor jwtPropagationInterceptor) {

        return builder.baseUrl(properties.baseUrl())
                .requestInterceptor(jwtPropagationInterceptor)
                .build();
    }

    @Bean(name = "inventoryRestClient")
    RestClient inventoryRestClient(
            RestClient.Builder builder,
            InventoryServiceProperties properties,
            JwtPropagationInterceptor jwtPropagationInterceptor) {

        return builder.baseUrl(properties.baseUrl())
                .requestInterceptor(jwtPropagationInterceptor)
                .build();
    }

    @Bean(name = "paymentRestClient")
    RestClient paymentRestClient(
            RestClient.Builder builder,
            PaymentServiceProperties properties,
            JwtPropagationInterceptor jwtPropagationInterceptor) {

        return builder.baseUrl(properties.baseUrl())
                .requestInterceptor(jwtPropagationInterceptor)
                .build();
    }

    @Bean(name = "shippingRestClient")
    RestClient shippingRestClient(
            RestClient.Builder builder,
            ShippingServiceProperties properties,
            JwtPropagationInterceptor jwtPropagationInterceptor) {

        return builder.baseUrl(properties.baseUrl())
                .requestInterceptor(jwtPropagationInterceptor)
                .build();
    }

}
