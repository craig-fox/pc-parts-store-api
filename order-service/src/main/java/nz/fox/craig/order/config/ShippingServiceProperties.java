package nz.fox.craig.order.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "services.shipping")
public record ShippingServiceProperties(String baseUrl) {}
