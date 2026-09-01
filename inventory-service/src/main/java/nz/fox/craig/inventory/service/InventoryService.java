package nz.fox.craig.inventory.service;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import nz.fox.craig.inventory.dto.InventoryResponse;
import nz.fox.craig.inventory.exception.InventoryNotFoundException;
import nz.fox.craig.inventory.mapper.InventoryMapper;
import nz.fox.craig.inventory.metrics.InventoryMetrics;
import nz.fox.craig.inventory.model.Inventory;
import nz.fox.craig.inventory.repository.InventoryRepository;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class InventoryService {

    private final InventoryRepository inventoryRepository;
    private final InventoryMapper inventoryMapper;
    private final InventoryMetrics inventoryMetrics;

    public InventoryResponse getInventory(UUID productId) {
        Inventory inventory = findInventory(productId);

        return inventoryMapper.toResponse(inventory);
    }

    public InventoryResponse reserveStock(UUID productId, int quantity) {
        Inventory inventory = findInventory(productId);

        inventory.reserve(quantity);
        inventoryRepository.save(inventory);
        inventoryMetrics.reservationMade();
        return inventoryMapper.toResponse(inventory);
    }

    public InventoryResponse releaseReservation(UUID productId, int quantity) {
        Inventory inventory = findInventory(productId);

        inventory.release(quantity);
        inventoryRepository.save(inventory);
        inventoryMetrics.releaseMade();
        return inventoryMapper.toResponse(inventory);
    }

    public InventoryResponse confirmReservation(UUID productId, int quantity) {
        Inventory inventory = findInventory(productId);

        inventory.confirmReservation(quantity);

        inventoryRepository.save(inventory);

        return inventoryMapper.toResponse(inventory);
    }

    private Inventory findInventory(UUID productId) {
        return inventoryRepository
                .findById(productId)
                .orElseThrow(() -> new InventoryNotFoundException(productId));
    }
}
