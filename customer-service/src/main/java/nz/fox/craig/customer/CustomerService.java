package nz.fox.craig.customer;

import java.util.List;
import lombok.RequiredArgsConstructor;
import nz.fox.craig.customer.dto.CustomerRequest;
import nz.fox.craig.customer.dto.CustomerResponse;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CustomerService {

	private final CustomerRepository customerRepository;

	@Transactional
	public CustomerResponse createCustomer(CustomerRequest request) {
		Customer customer = Customer.builder()
				.name(request.name())
				.email(request.email())
				.address(request.address())
				.build();
		return CustomerResponse.from(customerRepository.save(customer));
	}

	@Transactional(readOnly = true)
	public List<CustomerResponse> getAllCustomers() {
		return customerRepository.findAll().stream()
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

}
