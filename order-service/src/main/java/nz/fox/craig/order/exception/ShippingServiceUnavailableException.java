package nz.fox.craig.order.exception;

public class ShippingServiceUnavailableException
        extends RuntimeException {

    public ShippingServiceUnavailableException(Throwable cause) {
        super("Shipping service is unavailable", cause);
    }
}
