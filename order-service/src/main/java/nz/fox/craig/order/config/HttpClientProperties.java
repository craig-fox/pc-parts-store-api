package nz.fox.craig.order.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "services.http-client")
public record HttpClientProperties(
        Duration connectTimeout,
        Duration readTimeout) {}
