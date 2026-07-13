package nz.fox.craig.customer.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CustomerRequest(

	@NotBlank(message = "Name is required")
	@Size(min = 2, max = 100,
		  message = "Name must be between 2 and 100 characters")
	String name,

	@NotBlank(message = "Email is required")
	@Email(message = "Email must be a valid email address")
	@Size(max = 255,
		  message = "Email must not exceed 255 characters")
	String email,

	@NotBlank(message = "Address is required")
	@Size(min = 5, max = 255,
		  message = "Address must be between 5 and 255 characters")
	String address

) {
}
