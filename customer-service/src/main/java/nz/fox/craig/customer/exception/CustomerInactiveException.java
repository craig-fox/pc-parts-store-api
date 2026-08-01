package nz.fox.craig.customer.exception;

public class CustomerInactiveException extends BusinessException {

    public CustomerInactiveException() {
        super("Customer account is inactive");
    }

}
