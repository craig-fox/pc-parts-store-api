package nz.fox.craig.customer.exception;

public class CustomerInactiveException extends BusinessException {

    public CustomerInactiveException(Long id) {
        super("Customer is inactive: " + id);
    }

}
