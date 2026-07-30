package nz.fox.craig.customer.service;

import java.util.List;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import nz.fox.craig.customer.dto.CustomerRequest;
import nz.fox.craig.customer.dto.CustomerResponse;
import nz.fox.craig.customer.exception.CustomerAlreadyExistsException;
import nz.fox.craig.customer.exception.CustomerNotFoundException;
import nz.fox.craig.customer.model.Customer;
import nz.fox.craig.customer.model.CustomerStatus;
import nz.fox.craig.customer.repository.CustomerRepository;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CustomerService {

	private final CustomerRepository customerRepository;
	private final PasswordEncoder passwordEncoder;

	@Transactional
	public CustomerResponse createCustomer(CustomerRequest request) {
		if (customerRepository.findByEmail(request.email()).isPresent()) {
			throw new CustomerAlreadyExistsException(request.email());
		}


		final Customer customer = Customer.builder()
				.firstName(request.firstName())
				.lastName(request.lastName())
				.preferredName(request.preferredName())
				.email(request.email())
				.address(request.address())
				.password(passwordEncoder.encode(request.password()))
				.status(CustomerStatus.ACTIVE)
				.build();
		return CustomerResponse.from(customerRepository.save(customer));
	}


	public List<CustomerResponse> getCustomers(CustomerStatus status) {

		final List<Customer> customers;
	
		if (status == null) {
			customers = customerRepository.findAll();
		} else {
			customers = customerRepository.findByStatus(status);
		}
	
		return customers.stream()
				.map(CustomerResponse::from)
				.toList();
	}

	@Transactional(readOnly = true)
	public CustomerResponse getCustomer(UUID id) {
		return customerRepository.findById(id)
				.map(CustomerResponse::from)
				.orElseThrow(() -> new CustomerNotFoundException(id));
	}

	@Transactional
	public CustomerResponse updateCustomer(UUID id, CustomerRequest request) {
		final Customer customer = customerRepository.findById(id)
				.orElseThrow(() -> new CustomerNotFoundException(id));
		customer.setFirstName(request.firstName());
		customer.setLastName(request.lastName());
		customer.setEmail(request.email());
		customer.setAddress(request.address());
		return CustomerResponse.from(customerRepository.save(customer));
	}

	@Transactional
	public void deactivateCustomer(UUID id) {
		final Customer customer = customerRepository.findById(id)

            .orElseThrow(() -> new CustomerNotFoundException(id));

		customer.setStatus(CustomerStatus.INACTIVE);
		customerRepository.save(customer);
	}

	@Transactional
	public void activateCustomer(UUID id) {
		final Customer customer = customerRepository.findById(id)
				.orElseThrow(() -> new CustomerNotFoundException(id));
		if (customer.getStatus() == CustomerStatus.ACTIVE) {
			throw new CustomerAlreadyExistsException(customer.getEmail());
		}
		customer.setStatus(CustomerStatus.ACTIVE);
		customerRepository.save(customer);
	}

}
