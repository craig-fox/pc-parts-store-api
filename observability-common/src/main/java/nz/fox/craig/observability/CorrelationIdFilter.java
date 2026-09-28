package nz.fox.craig.observability;

import java.io.IOException;
import java.util.UUID;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.core.Ordered;

@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdFilter extends OncePerRequestFilter {

    private String resolveCorrelationId(HttpServletRequest request) {
        String incoming = request.getHeader(CorrelationId.HEADER);
    
        if (incoming != null && incoming.length() <= 36) {
            try {
                return UUID.fromString(incoming).toString();
            } catch (IllegalArgumentException ignored) {
                // Invalid ID; generate a new one.
            }
        }
    
        return UUID.randomUUID().toString();
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        String correlationId = resolveCorrelationId(request);

        try {
            MDC.put(CorrelationId.MDC_KEY, correlationId);
            response.setHeader(CorrelationId.HEADER, correlationId);
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(CorrelationId.MDC_KEY);
        }
    }
}
