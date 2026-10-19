package nz.fox.craig.inventory.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class InventoryMetrics {

    private final Counter reservationsMade;
    private final Counter releasesMade;

    public InventoryMetrics(MeterRegistry meterRegistry) {
        reservationsMade =
                Counter.builder("inventory.reservations")
                        .description("Number of reservations successfully made")
                        .register(meterRegistry);

        releasesMade =
                Counter.builder("inventory.releases")
                        .description("Number of releases successfully made")
                        .register(meterRegistry);
    }

    public void reservationMade() {
        reservationsMade.increment();
    }

    public void releaseMade() {
        releasesMade.increment();
    }
}
