package nz.fox.craig.customer.controller;

import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import nz.fox.craig.customer.dto.CustomerAuthenticationResponse;
import nz.fox.craig.customer.dto.CustomerRequest;
import nz.fox.craig.customer.dto.CustomerResponse;
import nz.fox.craig.customer.model.CustomerStatus;
import nz.fox.craig.customer.service.CustomerService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Customers", description = "Operations for managing customers")
@RestController
@RequestMapping("/api/customers")
@RequiredArgsConstructor
public class CustomerController {

	private final CustomerService customerService;

	@Operation(summary = "Register a new customer")
	@ApiResponses({
		@ApiResponse(responseCode = "201", description = "Customer registered"),
		@ApiResponse(responseCode = "400", description = "Validation failed")
	})
	@PostMapping
	public ResponseEntity<CustomerResponse> createCustomer(@Valid @RequestBody CustomerRequest request) {
		CustomerResponse response = customerService.registerCustomer(request);
		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}


	@Operation(summary = "Get customers of a given status, or all customers")
	@GetMapping
	public List<CustomerResponse> getCustomers(
			@RequestParam(required = false) CustomerStatus status) {
		return customerService.getCustomers(status);
	}

	@Operation(summary = "Deactivate customer")
	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void deactivateCustomer(@PathVariable UUID id) {
		customerService.deactivateCustomer(id);
	}

	@Operation(summary = "Update details of a customer by ID")
	@PutMapping("/{id}")
	public CustomerResponse updateCustomer(@PathVariable UUID id, @Valid @RequestBody CustomerRequest request) {
		return customerService.updateCustomer(id, request);
	}

	@Operation(summary = "Retrieve details of a customer by ID")
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "Customer found"),
		@ApiResponse(responseCode = "404", description = "Customer not found")
	})
	@GetMapping("/{id}")
	public CustomerResponse getCustomer(@PathVariable UUID id) {
		return customerService.getCustomer(id);
	}

	@Operation(summary = "Retrieve details of a customer by email")
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "Customer found"),
		@ApiResponse(responseCode = "404", description = "Customer not found")
	})
	@GetMapping("/email/{email}")
	public CustomerAuthenticationResponse getCustomerByEmail(@PathVariable String email) {
		return customerService.getCustomerByEmail(email);
	}

	@Operation(summary = "Reactivate an inactive customer")
	@PutMapping("/{id}/activate")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void activateCustomer(@PathVariable UUID id) {
		customerService.activateCustomer(id);
	}

	@RequestMapping(method = RequestMethod.HEAD, path = "/{id}")
	public ResponseEntity<Void> customerExists(@PathVariable UUID id) {
		customerService.getCustomer(id);
		return ResponseEntity.ok().build();
	}
}
