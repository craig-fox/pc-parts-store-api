package nz.fox.craig.order.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "services.customer")
public record CustomerServiceProperties(String baseUrl) { }
