package nz.fox.craig.customer.dto;

import java.util.UUID;

import nz.fox.craig.customer.model.Customer;
import nz.fox.craig.customer.model.CustomerStatus;

public record CustomerAuthenticationResponse(
    UUID id,
    String email,
    String password,
    CustomerStatus status
) {

    public static CustomerAuthenticationResponse from(Customer customer) {
        return new CustomerAuthenticationResponse(
                customer.getId(),
                customer.getEmail(),
                customer.getPassword(),
                customer.getStatus()
        );
    }
}
