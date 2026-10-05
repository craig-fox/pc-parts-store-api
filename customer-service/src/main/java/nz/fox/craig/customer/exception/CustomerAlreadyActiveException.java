package nz.fox.craig.customer.exception;

public class CustomerAlreadyActiveException extends BusinessException {

    public CustomerAlreadyActiveException(String email) {
        super("Customer with email " + email + " is already active.");
    }

}
