package nz.fox.craig.order.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import nz.fox.craig.observability.CorrelationIdFilter;
import nz.fox.craig.observability.CorrelationIdInterceptor;
import nz.fox.craig.observability.RestClientLoggingInterceptor;

@Configuration
public class ObservabilityConfig {

    @Bean
    CorrelationIdFilter correlationIdFilter() {
        return new CorrelationIdFilter();
    }

    @Bean
    CorrelationIdInterceptor correlationIdInterceptor() {
        return new CorrelationIdInterceptor();
    }

    @Bean
    RestClientLoggingInterceptor restClientLoggingInterceptor() {
        return new RestClientLoggingInterceptor();
    }
}
