package nz.fox.craig.customer.dto;

import java.util.UUID;

import nz.fox.craig.customer.model.Customer;
import nz.fox.craig.customer.model.CustomerStatus;

public record CustomerResponse(
		UUID id,
		String firstName,
		String lastName,
		String displayName,
		String email,
		String address,
		CustomerStatus status
) {

	public static CustomerResponse from(Customer customer) {
		return new CustomerResponse(
				customer.getId(),
				customer.getFirstName(),
				customer.getLastName(),
				resolveDisplayName(customer),
				customer.getEmail(),
				customer.getAddress(),
				customer.getStatus()
		);
	}

	private static String resolveDisplayName(Customer customer) {
		String preferredName = customer.getPreferredName();
		return (preferredName != null && !preferredName.isBlank())
				? preferredName
				: customer.getFirstName();
	}

}
