package nz.fox.craig.order.client;

import java.util.UUID;

public interface InventoryClient {
    void reserveStock(UUID productId, int quantity);

    void releaseStock(UUID productId, int quantity);
}
