package nz.fox.craig.order.exception;

import java.util.UUID;

public class ProductNotFoundException extends RuntimeException {
    public ProductNotFoundException(UUID id) {
		super(String.format( "Product %s not found", id));
	}
}
