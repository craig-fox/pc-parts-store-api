package nz.fox.craig.customer.dto;

import java.util.UUID;

import nz.fox.craig.customer.model.Customer;

public record CustomerResponse(
		UUID id,
		String name,
		String email,
		String address
) {

	public static CustomerResponse from(Customer customer) {
		return new CustomerResponse(
				customer.getId(),
				customer.getName(),
				customer.getEmail(),
				customer.getAddress()
		);
	}

}
