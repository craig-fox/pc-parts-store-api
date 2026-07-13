package nz.fox.craig.customer.dto;

import nz.fox.craig.customer.Customer;

public record CustomerResponse(
		Long id,
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
