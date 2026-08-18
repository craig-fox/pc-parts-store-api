package nz.fox.craig.order.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "services.product")
public record ProductServiceProperties(String baseUrl) {}
