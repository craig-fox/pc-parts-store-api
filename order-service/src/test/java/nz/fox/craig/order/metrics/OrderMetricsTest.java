package nz.fox.craig.order.metrics;

import static org.assertj.core.api.Assertions.assertThat;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class OrderMetricsTest {

    private SimpleMeterRegistry meterRegistry;
    private OrderMetrics orderMetrics;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        orderMetrics = new OrderMetrics(meterRegistry);
    }

    @Test
    void incrementsOrdersCreatedCounter() {
        orderMetrics.orderCreated();

        assertThat(
                meterRegistry
                        .counter("orders.created")
                        .count())
                .isEqualTo(1.0);
    }

    @Test
    void incrementsOrdersCancelledCounter() {
        orderMetrics.orderCancelled();

        assertThat(
                meterRegistry
                        .counter("orders.cancelled")
                        .count())
                .isEqualTo(1.0);
    }

    @Test
    void incrementsOrdersCreatedCounterEachTimeCalled() {
        orderMetrics.orderCreated();
        orderMetrics.orderCreated();

        assertThat(
                meterRegistry
                        .counter("orders.created")
                        .count())
                .isEqualTo(2.0);
    }
}
