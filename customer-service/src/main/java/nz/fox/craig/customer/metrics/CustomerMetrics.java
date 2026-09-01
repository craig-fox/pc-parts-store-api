package nz.fox.craig.customer.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;

import org.springframework.stereotype.Component;

@Component
public class CustomerMetrics {

    private final Counter customersRegistered;
    private final Counter customersActivated;
    private final Counter customersDeactivated;

    public CustomerMetrics(MeterRegistry meterRegistry) {
        customersRegistered = Counter.builder("customers.registered")
                .description("Number of customers successfully registered")
                .register(meterRegistry);

        customersActivated = Counter.builder("customers.activated")
                .description("Number of customers successfully activated")
                .register(meterRegistry);

        customersDeactivated = Counter.builder("customers.deactivated")
                .description("Number of customers successfully deactivated")
                .register(meterRegistry);
    }

    public void customerRegistered() {
        customersRegistered.increment();
    }

    public void customerActivated() {
        customersActivated.increment();
    }

    public void customerDeactivated() {
        customersDeactivated.increment();
    }
}
