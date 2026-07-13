package nz.fox.craig.customer.exception;

public class CustomerAlreadyExistsException extends BusinessException {

    public CustomerAlreadyExistsException(String email) {
        super("Customer already exists with email: " + email);
    }

}
