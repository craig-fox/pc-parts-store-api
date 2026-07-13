package nz.fox.craig.customer.service;

import java.util.List;
import lombok.RequiredArgsConstructor;
import nz.fox.craig.customer.dto.CustomerRequest;
import nz.fox.craig.customer.dto.CustomerResponse;
import nz.fox.craig.customer.exception.CustomerAlreadyExistsException;
import nz.fox.craig.customer.exception.CustomerNotFoundException;
import nz.fox.craig.customer.model.Customer;
import nz.fox.craig.customer.model.CustomerStatus;
import nz.fox.craig.customer.repository.CustomerRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CustomerService {

	private final CustomerRepository customerRepository;

	@Transactional
	public CustomerResponse createCustomer(CustomerRequest request) {
		if(customerRepository.findByEmail(request.email()).isPresent()) {
			throw new CustomerAlreadyExistsException(request.email());
		}
		Customer customer = Customer.builder()
				.name(request.name())
				.email(request.email())
				.address(request.address())
				.status(CustomerStatus.ACTIVE)
				.build();
		return CustomerResponse.from(customerRepository.save(customer));
	}


	public List<CustomerResponse> getCustomers(CustomerStatus status) {

		List<Customer> customers;
	
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
	public CustomerResponse getCustomer(Long id) {
		return customerRepository.findById(id)
				.map(CustomerResponse::from)
				.orElseThrow(() -> new CustomerNotFoundException(id));
	}

	@Transactional
	public CustomerResponse updateCustomer(Long id, CustomerRequest request) {
		Customer customer = customerRepository.findById(id)
				.orElseThrow(() -> new CustomerNotFoundException(id));
		customer.setName(request.name());
		customer.setEmail(request.email());
		customer.setAddress(request.address());
		return CustomerResponse.from(customerRepository.save(customer));
	}

	@Transactional
	public void deactivateCustomer(Long id) {
		Customer customer = customerRepository.findById(id)

            .orElseThrow(() -> new CustomerNotFoundException(id));

		customer.setStatus(CustomerStatus.INACTIVE);
		customerRepository.save(customer);
	}

	@Transactional
	public void activateCustomer(Long id) {
		Customer customer = customerRepository.findById(id)
				.orElseThrow(() -> new CustomerNotFoundException(id));
		if(customer.getStatus() == CustomerStatus.ACTIVE) {
			throw new CustomerAlreadyExistsException(customer.getEmail());
		}
		customer.setStatus(CustomerStatus.ACTIVE);
		customerRepository.save(customer);
	}

}
