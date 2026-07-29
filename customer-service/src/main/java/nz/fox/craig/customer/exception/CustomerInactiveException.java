package nz.fox.craig.customer.exception;

import java.util.UUID;

public class CustomerInactiveException extends BusinessException {

    public CustomerInactiveException(UUID id) {
        super("Customer is inactive: " + id);
    }

}
