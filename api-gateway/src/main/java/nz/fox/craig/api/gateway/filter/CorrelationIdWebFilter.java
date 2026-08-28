package nz.fox.craig.api.gateway.filter;

import java.util.UUID;

import nz.fox.craig.observability.CorrelationId;

import org.springframework.stereotype.Component;

import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;

import reactor.core.publisher.Mono;

@Component
public class CorrelationIdWebFilter implements WebFilter {

    @Override
    public Mono<Void> filter(
            ServerWebExchange exchange,
            WebFilterChain chain) {

        String correlationId = exchange.getRequest()
                .getHeaders()
                .getFirst(CorrelationId.HEADER);

        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
        }

        String finalCorrelationId = correlationId;

        exchange.getResponse()
                .getHeaders()
                .set(CorrelationId.HEADER, finalCorrelationId);

        return chain.filter(exchange)
                .contextWrite(context ->
                        context.put(
                                CorrelationId.MDC_KEY,
                                finalCorrelationId));
    }
}
