package nz.fox.craig.inventory.metrics;

import static org.assertj.core.api.Assertions.assertThat;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class InventoryMetricsTest {

    private SimpleMeterRegistry meterRegistry;
    private InventoryMetrics inventoryMetrics;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        inventoryMetrics = new InventoryMetrics(meterRegistry);
    }

    @Test
    void incrementsReservationsCounter() {
        inventoryMetrics.reservationMade();

        assertThat(meterRegistry.counter("inventory.reservations").count())
                .isEqualTo(1.0);
    }

    @Test
    void incrementsReleasesCounter() {
        inventoryMetrics.releaseMade();

        assertThat(meterRegistry.counter("inventory.releases").count())
                .isEqualTo(1.0);
    }

    @Test
    void incrementsReservationsCounterEachTimeCalled() {
        inventoryMetrics.reservationMade();
        inventoryMetrics.reservationMade();

        assertThat(meterRegistry.counter("inventory.reservations").count())
                .isEqualTo(2.0);
    }

    @Test
    void incrementsReleasesCounterEachTimeCalled() {
        inventoryMetrics.releaseMade();
        inventoryMetrics.releaseMade();

        assertThat(meterRegistry.counter("inventory.releases").count())
                .isEqualTo(2.0);
    }
}
