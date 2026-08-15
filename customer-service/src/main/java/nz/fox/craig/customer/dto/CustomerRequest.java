package nz.fox.craig.customer.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CustomerRequest(
        @NotBlank(message = "First name is required")
                @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
                String firstName,
        @NotBlank(message = "Last name is required")
                @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
                String lastName,
        @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
                String preferredName,
        @NotBlank(message = "Email is required")
                @Email(message = "Email must be a valid email address")
                @Size(max = 255, message = "Email must not exceed 255 characters")
                String email,
        @NotBlank(message = "Address is required")
                @Size(min = 5, max = 255, message = "Address must be between 5 and 255 characters")
                String address,
        @NotBlank(message = "Password is required")
                @Size(min = 8, max = 50, message = "Password must be between 8 and 50 characters")
                String password) { }
