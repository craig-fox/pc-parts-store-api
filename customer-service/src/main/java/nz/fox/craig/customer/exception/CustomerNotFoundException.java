package nz.fox.craig.customer.exception;

import java.util.UUID;

public class CustomerNotFoundException extends BusinessException {

	public CustomerNotFoundException(UUID id) {
		super("Customer not found with id: " + id);
	}

	public CustomerNotFoundException(String email) {
        super("Customer not found with email: " + email);
    }

}
