package nz.fox.craig.inventory.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;
import nz.fox.craig.inventory.exception.InsufficientInventoryException;
import org.junit.jupiter.api.Test;

class InventoryTest {

    @Test
    void shouldReturnAvailableQuantity() {
        Inventory inventory = new Inventory(UUID.randomUUID(), 10, 3);

        assertThat(inventory.getAvailableQuantity()).isEqualTo(7);
    }

    @Test
    void shouldReserveInventory() {
        Inventory inventory = new Inventory(UUID.randomUUID(), 10, 2);

        inventory.reserve(3);

        assertThat(inventory.getQuantityReserved()).isEqualTo(5);
        assertThat(inventory.getAvailableQuantity()).isEqualTo(5);
    }

    @Test
    void shouldRejectReservationWhenQuantityIsZero() {
        Inventory inventory = new Inventory(UUID.randomUUID(), 10, 2);

        assertThatThrownBy(() -> inventory.reserve(0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Quantity must be greater than zero.");
    }

    @Test
    void shouldRejectReservationWhenQuantityIsNegative() {
        Inventory inventory = new Inventory(UUID.randomUUID(), 10, 2);

        assertThatThrownBy(() -> inventory.reserve(-1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Quantity must be greater than zero.");
    }

    @Test
    void shouldRejectReservationWhenQuantityExceedsAvailableInventory() {
        UUID productId = UUID.randomUUID();
        Inventory inventory = new Inventory(productId, 10, 2);

        assertThatThrownBy(() -> inventory.reserve(9))
                .isInstanceOf(InsufficientInventoryException.class);
    }

    @Test
    void shouldReleaseInventory() {
        Inventory inventory = new Inventory(UUID.randomUUID(), 10, 5);

        inventory.release(3);

        assertThat(inventory.getQuantityReserved()).isEqualTo(2);
        assertThat(inventory.getAvailableQuantity()).isEqualTo(8);
    }

    @Test
    void shouldRejectReleaseWhenQuantityIsZero() {
        Inventory inventory = new Inventory(UUID.randomUUID(), 10, 5);

        assertThatThrownBy(() -> inventory.release(0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Quantity must be greater than zero.");
    }

    @Test
    void shouldRejectReleaseWhenQuantityIsNegative() {
        Inventory inventory = new Inventory(UUID.randomUUID(), 10, 5);

        assertThatThrownBy(() -> inventory.release(-1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Quantity must be greater than zero.");
    }

    @Test
    void shouldRejectReleaseWhenQuantityExceedsReservedQuantity() {
        Inventory inventory = new Inventory(UUID.randomUUID(), 10, 5);

        assertThatThrownBy(() -> inventory.release(6))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Cannot release more stock than is reserved.");
    }

    @Test
    void shouldConfirmReservation() {
        Inventory inventory = new Inventory(UUID.randomUUID(), 10, 5);

        inventory.confirmReservation(3);

        assertThat(inventory.getQuantityReserved()).isEqualTo(2);
        assertThat(inventory.getQuantityOnHand()).isEqualTo(7);
        assertThat(inventory.getAvailableQuantity()).isEqualTo(5);
    }

    @Test
    void shouldRejectConfirmationWhenQuantityIsZero() {
        Inventory inventory = new Inventory(UUID.randomUUID(), 10, 5);

        assertThatThrownBy(() -> inventory.confirmReservation(0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Quantity must be greater than zero.");
    }

    @Test
    void shouldRejectConfirmationWhenQuantityIsNegative() {
        Inventory inventory = new Inventory(UUID.randomUUID(), 10, 5);

        assertThatThrownBy(() -> inventory.confirmReservation(-1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Quantity must be greater than zero.");
    }

    @Test
    void shouldRejectConfirmationWhenQuantityExceedsReservedQuantity() {
        Inventory inventory = new Inventory(UUID.randomUUID(), 10, 5);

        assertThatThrownBy(() -> inventory.confirmReservation(6))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Cannot confirm more stock than is reserved.");
    }

    @Test
    void shouldReturnOutOfStockWhenQuantityOnHandIsZero() {
        Inventory inventory = new Inventory(UUID.randomUUID(), 0, 0);

        assertThat(inventory.getStatus())
                .isEqualTo(InventoryStatus.OUT_OF_STOCK);
    }

    @Test
    void shouldReturnLowStockWhenAvailableQuantityIsFiveOrLess() {
        Inventory inventory = new Inventory(UUID.randomUUID(), 10, 5);

        assertThat(inventory.getStatus())
                .isEqualTo(InventoryStatus.LOW_STOCK);
    }

    @Test
    void shouldReturnInStockWhenAvailableQuantityIsGreaterThanFive() {
        Inventory inventory = new Inventory(UUID.randomUUID(), 10, 2);

        assertThat(inventory.getStatus())
                .isEqualTo(InventoryStatus.IN_STOCK);
    }
}
