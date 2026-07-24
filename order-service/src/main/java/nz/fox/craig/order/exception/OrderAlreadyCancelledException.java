package nz.fox.craig.order.exception;

import java.util.UUID;

public class OrderAlreadyCancelledException extends RuntimeException {

	public OrderAlreadyCancelledException(UUID id) {
		super(String.format( "Order %s is already cancelled", id));
	}

}
