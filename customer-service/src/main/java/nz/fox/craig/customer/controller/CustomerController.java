package nz.fox.craig.customer.controller;

import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/customers")
@RequiredArgsConstructor
public class CustomerController {

	private final CustomerService customerService;

	@PostMapping
	public ResponseEntity<CustomerResponse> createCustomer(@Valid @RequestBody CustomerRequest request) {
		CustomerResponse response = customerService.createCustomer(request);
		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}


	@GetMapping
	public List<CustomerResponse> getCustomers(
			@RequestParam(required = false) CustomerStatus status) {
		return customerService.getCustomers(status);
	}

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void deactivateCustomer(@PathVariable Long id) {
		customerService.deactivateCustomer(id);
	}

	@PutMapping("/{id}")
	public CustomerResponse updateCustomer(@PathVariable Long id, @Valid @RequestBody CustomerRequest request) {
		return customerService.updateCustomer(id, request);
	}

	@GetMapping("/{id}")
	public CustomerResponse getCustomer(@PathVariable Long id) {
		return customerService.getCustomer(id);
	}

	@PutMapping("/{id}/activate")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void activateCustomer(@PathVariable Long id) {
		customerService.activateCustomer(id);
	}
}
