package nz.fox.craig.shipping.exception;

import java.util.UUID;

public class ShipmentNotFoundException extends RuntimeException {

    public ShipmentNotFoundException(UUID id) {
        super("Shipment not found: " + id);
    }
}
