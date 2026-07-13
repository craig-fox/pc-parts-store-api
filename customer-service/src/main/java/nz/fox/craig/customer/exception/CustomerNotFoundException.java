package nz.fox.craig.customer.exception;

public class CustomerNotFoundException extends BusinessException {

	public CustomerNotFoundException(Long id) {
		super("Customer not found with id: " + id);
	}

}
