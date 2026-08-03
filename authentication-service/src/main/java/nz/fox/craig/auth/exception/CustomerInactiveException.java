package nz.fox.craig.auth.exception;


public class CustomerInactiveException extends RuntimeException {

    public CustomerInactiveException() {
        super("Customer account is inactive");
    }

}
