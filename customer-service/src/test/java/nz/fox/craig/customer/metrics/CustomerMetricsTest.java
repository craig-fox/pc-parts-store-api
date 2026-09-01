package nz.fox.craig.customer.metrics;

import static org.assertj.core.api.Assertions.assertThat;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CustomerMetricsTest {

    private SimpleMeterRegistry meterRegistry;
    private CustomerMetrics customerMetrics;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        customerMetrics = new CustomerMetrics(meterRegistry);
    }

    @Test
    void incrementsCustomersRegisteredCounter() {
        customerMetrics.customerRegistered();

        assertThat(
                meterRegistry
                        .counter("customers.registered")
                        .count())
                .isEqualTo(1.0);
    }

    @Test
    void incrementsCustomersActivatedCounter() {
        customerMetrics.customerActivated();

        assertThat(
                meterRegistry
                        .counter("customers.activated")
                        .count())
                .isEqualTo(1.0);
    }

    @Test
    void incrementsCustomersDeactivatedCounter() {
        customerMetrics.customerDeactivated();

        assertThat(
                meterRegistry
                        .counter("customers.deactivated")
                        .count())
                .isEqualTo(1.0);
    }
}
