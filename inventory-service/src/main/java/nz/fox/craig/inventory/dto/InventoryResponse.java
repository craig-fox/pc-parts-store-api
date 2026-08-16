package nz.fox.craig.inventory.dto;

import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Builder;
import nz.fox.craig.inventory.model.InventoryStatus;

@Builder
public record InventoryResponse(
        UUID productId,
        int quantityOnHand,
        int quantityReserved,
        int availableQuantity,
        InventoryStatus status,
        LocalDateTime lastUpdated) { }
