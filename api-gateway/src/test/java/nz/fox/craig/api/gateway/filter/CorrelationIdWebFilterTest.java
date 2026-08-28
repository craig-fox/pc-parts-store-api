package nz.fox.craig.api.gateway.filter;

import static org.assertj.core.api.Assertions.assertThat;

import nz.fox.craig.observability.CorrelationId;

import org.junit.jupiter.api.Test;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.WebFilterChain;

import reactor.core.publisher.Mono;

class CorrelationIdWebFilterTest {

    private final CorrelationIdWebFilter filter =
            new CorrelationIdWebFilter();

    @Test
    void preservesExistingCorrelationId() {
        String correlationId = "test-correlation-123";

        MockServerHttpRequest request = MockServerHttpRequest
                .get("/test")
                .header(CorrelationId.HEADER, correlationId)
                .build();

        MockServerWebExchange exchange =
                MockServerWebExchange.from(request);

        WebFilterChain chain = exchange1 -> Mono.empty();

        filter.filter(exchange, chain).block();

        assertThat(exchange.getResponse()
                .getHeaders()
                .getFirst(CorrelationId.HEADER))
                .isEqualTo(correlationId);
    }

    @Test
    void generatesCorrelationIdWhenHeaderIsMissing() {
        MockServerHttpRequest request = MockServerHttpRequest
                .get("/test")
                .build();

        MockServerWebExchange exchange =
                MockServerWebExchange.from(request);

        WebFilterChain chain = exchange1 -> Mono.empty();

        filter.filter(exchange, chain).block();

        String correlationId = exchange.getResponse()
                .getHeaders()
                .getFirst(CorrelationId.HEADER);

        assertThat(correlationId)
                .isNotNull()
                .isNotBlank()
                .matches("[0-9a-fA-F-]{36}");
    }

    @Test
    void makesCorrelationIdAvailableInReactiveContext() {
        String correlationId = "test-correlation-123";

        MockServerHttpRequest request = MockServerHttpRequest
                .get("/test")
                .header(CorrelationId.HEADER, correlationId)
                .build();

        MockServerWebExchange exchange =
                MockServerWebExchange.from(request);

        WebFilterChain chain = exchange1 ->
            Mono.deferContextual(context -> {
    
                String actualCorrelationId =
                        context.get(CorrelationId.MDC_KEY);
    
                assertThat(actualCorrelationId)
                        .isEqualTo(correlationId);
    
                return Mono.empty();
            });

        filter.filter(exchange, chain).block();
    }
}
