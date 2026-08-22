package nz.fox.craig.order.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "services.payment")
public record PaymentServiceProperties(String baseUrl) {}
