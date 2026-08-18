package nz.fox.craig.inventory.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import nz.fox.craig.inventory.dto.InventoryResponse;
import nz.fox.craig.inventory.model.Inventory;
import nz.fox.craig.inventory.model.InventoryStatus;
import org.junit.jupiter.api.Test;

class InventoryMapperTest {

    private final InventoryMapper inventoryMapper = new InventoryMapper();

    @Test
    void shouldMapInventoryToResponse() {
        UUID productId = UUID.randomUUID();
        LocalDateTime lastUpdated = LocalDateTime.now();

        Inventory inventory = new Inventory(productId, 10, 3);
        inventory.setLastUpdated(lastUpdated);

        InventoryResponse response = inventoryMapper.toResponse(inventory);

        assertThat(response.productId()).isEqualTo(productId);
        assertThat(response.quantityOnHand()).isEqualTo(10);
        assertThat(response.quantityReserved()).isEqualTo(3);
        assertThat(response.availableQuantity()).isEqualTo(7);
        assertThat(response.status()).isEqualTo(InventoryStatus.IN_STOCK);
        assertThat(response.lastUpdated()).isEqualTo(lastUpdated);
    }

    @Test
    void shouldMapInventoryListToResponseList() {
        UUID firstProductId = UUID.randomUUID();
        UUID secondProductId = UUID.randomUUID();

        Inventory firstInventory = new Inventory(firstProductId, 10, 2);
        Inventory secondInventory = new Inventory(secondProductId, 20, 5);

        List<InventoryResponse> responses =
                inventoryMapper.toResponseList(List.of(firstInventory, secondInventory));

        assertThat(responses).hasSize(2);

        assertThat(responses.get(0).productId()).isEqualTo(firstProductId);
        assertThat(responses.get(0).quantityOnHand()).isEqualTo(10);
        assertThat(responses.get(0).quantityReserved()).isEqualTo(2);
        assertThat(responses.get(0).availableQuantity()).isEqualTo(8);

        assertThat(responses.get(1).productId()).isEqualTo(secondProductId);
        assertThat(responses.get(1).quantityOnHand()).isEqualTo(20);
        assertThat(responses.get(1).quantityReserved()).isEqualTo(5);
        assertThat(responses.get(1).availableQuantity()).isEqualTo(15);
    }
}
