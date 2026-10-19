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
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class InventoryService {

    private final InventoryRepository inventoryRepository;
    private final InventoryMapper inventoryMapper;
    private final InventoryMetrics inventoryMetrics;

    @Transactional(readOnly = true)
    public InventoryResponse getInventory(UUID productId) {
        Inventory inventory = findInventory(productId);

        return inventoryMapper.toResponse(inventory);
    }

    @Transactional
    public InventoryResponse reserveStock(UUID productId, int quantity) {
        Inventory inventory = findInventory(productId);

        inventory.reserve(quantity);
        inventoryMetrics.reservationMade();
        return inventoryMapper.toResponse(inventory);
    }

    @Transactional
    public InventoryResponse releaseReservation(UUID productId, int quantity) {
        Inventory inventory = findInventory(productId);

        inventory.release(quantity);
        inventoryMetrics.releaseMade();
        return inventoryMapper.toResponse(inventory);
    }

    @Transactional
    public InventoryResponse confirmReservation(UUID productId, int quantity) {
        Inventory inventory = findInventory(productId);

        inventory.confirmReservation(quantity);
        return inventoryMapper.toResponse(inventory);
    }

    private Inventory findInventory(UUID productId) {
        return inventoryRepository
                .findById(productId)
                .orElseThrow(() -> new InventoryNotFoundException(productId));
    }
}
