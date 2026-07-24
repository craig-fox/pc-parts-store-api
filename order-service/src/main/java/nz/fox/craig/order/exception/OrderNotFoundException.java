package nz.fox.craig.order.exception;

import java.util.UUID;

public class OrderNotFoundException extends RuntimeException {

	public OrderNotFoundException(UUID id) {
		super(String.format( "Order %s not found", id));
	}

}
