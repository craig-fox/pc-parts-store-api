package nz.fox.craig.order.client;

import java.util.UUID;

public interface CustomerClient {

    void validateCustomerExists(UUID customerId);

}
