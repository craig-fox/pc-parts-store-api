package nz.fox.craig.order.exception;

import java.util.UUID;

/**
 * CustomerNotFoundException
 */
public class CustomerNotFoundException extends RuntimeException {
    public CustomerNotFoundException(UUID id) {
		super("Customer not found with id: " + id);
	}

}
