package nz.fox.craig.inventory.mapper;

import java.util.List;
import nz.fox.craig.inventory.dto.InventoryResponse;
import nz.fox.craig.inventory.model.Inventory;
import org.springframework.stereotype.Component;

@Component
public class InventoryMapper {

    public InventoryResponse toResponse(Inventory inventory) {
        return InventoryResponse.builder()
                .productId(inventory.getProductId())
                .quantityOnHand(inventory.getQuantityOnHand())
                .quantityReserved(inventory.getQuantityReserved())
                .availableQuantity(inventory.getAvailableQuantity())
                .lastUpdated(inventory.getLastUpdated())
                .status(inventory.getStatus())
                .build();
    }

    public List<InventoryResponse> toResponseList(List<Inventory> inventory) {
        return inventory.stream().map(this::toResponse).toList();
    }
}
