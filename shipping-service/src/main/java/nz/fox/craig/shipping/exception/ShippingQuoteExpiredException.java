package nz.fox.craig.shipping.exception;

import java.util.UUID;

public class ShippingQuoteExpiredException extends RuntimeException {

    public ShippingQuoteExpiredException(UUID id) {
        super("Shipping quote has expired: " + id);
    }
}
