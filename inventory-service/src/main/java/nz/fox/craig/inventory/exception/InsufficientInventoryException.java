package nz.fox.craig.inventory.exception;

import java.util.UUID;

public class InsufficientInventoryException extends RuntimeException {
    public InsufficientInventoryException(UUID productId) {
        super("Insufficient inventory for product id: " + productId);
    }
}
