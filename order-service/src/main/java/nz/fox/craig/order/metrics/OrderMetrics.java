package nz.fox.craig.order.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;

import org.springframework.stereotype.Component;

@Component
public class OrderMetrics {

    private final Counter ordersCreated;
    private final Counter ordersCancelled;

    public OrderMetrics(MeterRegistry meterRegistry) {
        ordersCreated = Counter.builder("orders.created")
                .description("Number of orders successfully created")
                .register(meterRegistry);

        ordersCancelled = Counter.builder("orders.cancelled")
                .description("Number of orders cancelled")
                .register(meterRegistry);
    }

    public void orderCreated() {
        ordersCreated.increment();
    }

    public void orderCancelled() {
        ordersCancelled.increment();
    }
}
