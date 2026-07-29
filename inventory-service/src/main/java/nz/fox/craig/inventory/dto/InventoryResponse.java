package nz.fox.craig.inventory.dto;

import java.time.LocalDateTime;
import java.util.UUID;

import nz.fox.craig.inventory.model.InventoryStatus;

public record InventoryResponse(
        UUID productId,
        int quantityOnHand,
        int quantityReserved,
        int availableQuantity,
        InventoryStatus status,
        LocalDateTime lastUpdated
) {
}
