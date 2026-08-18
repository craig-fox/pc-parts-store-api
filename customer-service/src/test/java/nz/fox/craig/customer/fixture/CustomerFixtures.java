package nz.fox.craig.customer.fixture;

import nz.fox.craig.customer.model.Customer;
import nz.fox.craig.customer.model.CustomerStatus;

import java.util.UUID;

public final class CustomerFixtures {

    private CustomerFixtures() {
    }

    public static Customer aCustomer() {
        return Customer.builder()
                .firstName("Test")
                .lastName("Customer")
                .preferredName("")
                .email("customer-" + UUID.randomUUID() + "@test.example")
                .address("123 Test Street")
                .password("test-password")
                .status(CustomerStatus.ACTIVE)
                .build();
    }
}
