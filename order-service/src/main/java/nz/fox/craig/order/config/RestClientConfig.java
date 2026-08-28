package nz.fox.craig.order.config;

import nz.fox.craig.observability.CorrelationIdInterceptor;
import nz.fox.craig.observability.RestClientLoggingInterceptor;
import nz.fox.craig.security.JwtPropagationInterceptor;

import java.net.http.HttpClient;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@EnableConfigurationProperties(HttpClientProperties.class)
@Configuration
public class RestClientConfig {

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

    

    @Bean(name = "customerRestClient")
    RestClient customerRestClient(
            RestClient.Builder builder,
            CustomerServiceProperties properties,
            JwtPropagationInterceptor interceptor,
            CorrelationIdInterceptor correlationIdInterceptor,
            RestClientLoggingInterceptor loggingInterceptor,
            ClientHttpRequestFactory requestFactory) {

    return createRestClient(
            builder,
            properties.baseUrl(),
            interceptor,
            correlationIdInterceptor,
            loggingInterceptor,
            requestFactory);
}

    @Bean(name = "productRestClient")
        RestClient productRestClient(
            RestClient.Builder builder,
            ProductServiceProperties properties,
            JwtPropagationInterceptor interceptor,
            CorrelationIdInterceptor correlationIdInterceptor,
            RestClientLoggingInterceptor loggingInterceptor,
            ClientHttpRequestFactory requestFactory) {

        return createRestClient(
                builder,
                properties.baseUrl(),
                interceptor,
                correlationIdInterceptor,
                loggingInterceptor,
                requestFactory);
    }

    @Bean(name = "inventoryRestClient")
        RestClient inventoryRestClient(
            RestClient.Builder builder,
            InventoryServiceProperties properties,
            JwtPropagationInterceptor interceptor,
            CorrelationIdInterceptor correlationIdInterceptor,
            RestClientLoggingInterceptor loggingInterceptor,
            ClientHttpRequestFactory requestFactory) {

        return createRestClient(
                builder,
                properties.baseUrl(),
                interceptor,
                correlationIdInterceptor,
                loggingInterceptor,
                requestFactory);
    }

    @Bean(name = "paymentRestClient")
    RestClient paymentRestClient(
            RestClient.Builder builder,
            PaymentServiceProperties properties,
            JwtPropagationInterceptor jwtInterceptor,
            CorrelationIdInterceptor correlationIdInterceptor,
            RestClientLoggingInterceptor loggingInterceptor,
            ClientHttpRequestFactory requestFactory) {
    
        return createRestClient(
                builder,
                properties.baseUrl(),
                jwtInterceptor,
                correlationIdInterceptor,
                loggingInterceptor,
                requestFactory);
    }

    @Bean(name = "shippingRestClient")
        RestClient shippingRestClient(
            RestClient.Builder builder,
            ShippingServiceProperties properties,
            JwtPropagationInterceptor interceptor,
            CorrelationIdInterceptor correlationIdInterceptor,
            RestClientLoggingInterceptor loggingInterceptor,
            ClientHttpRequestFactory requestFactory) {

        return createRestClient(
                builder,
                properties.baseUrl(),
                interceptor,
                correlationIdInterceptor,
                loggingInterceptor,
                requestFactory);
    }

    private RestClient createRestClient(
        RestClient.Builder builder,
        String baseUrl,
        JwtPropagationInterceptor jwtInterceptor,
        CorrelationIdInterceptor correlationIdInterceptor,
        RestClientLoggingInterceptor loggingInterceptor,
        ClientHttpRequestFactory requestFactory) {

    return builder
            .baseUrl(baseUrl)
            .requestInterceptor(jwtInterceptor)
            .requestInterceptor(correlationIdInterceptor)
            .requestInterceptor(loggingInterceptor)
            .requestFactory(requestFactory)
            .build();
}

}
