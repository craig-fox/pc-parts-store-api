package nz.fox.craig.auth.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;


@Configuration
public class RestClientConfig {

    @Bean
    RestClient restClient(RestClient.Builder builder, 
                            @Value("${customer-service.base-url}") String baseUrl) {
        return builder.build();
    }
}
