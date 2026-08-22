package nz.fox.craig.shipping.exception;

import java.util.UUID;

public class ShippingQuoteNotFoundException extends RuntimeException {

    public ShippingQuoteNotFoundException(UUID id) {
        super("Shipping quote not found: " + id);
    }
}
