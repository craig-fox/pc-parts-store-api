package nz.fox.craig.customer.security;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import nz.fox.craig.customer.dto.LoginRequest;
import nz.fox.craig.customer.dto.LoginResponse;
import nz.fox.craig.customer.exception.CustomerInactiveException;
import nz.fox.craig.customer.exception.InvalidCredentialsException;
import nz.fox.craig.customer.model.Customer;
import nz.fox.craig.customer.model.CustomerStatus;
import nz.fox.craig.customer.repository.CustomerRepository;
import nz.fox.craig.customer.service.TokenService;

@Service
@RequiredArgsConstructor
public class AuthenticationService {

    private final CustomerRepository customerRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;

    public LoginResponse login(LoginRequest request) {

        Customer customer = customerRepository.findByEmail(request.email())
                .orElseThrow(() ->
                        new InvalidCredentialsException());

        if (customer.getStatus() != CustomerStatus.ACTIVE) {
            throw new CustomerInactiveException();
        }

        if (!passwordEncoder.matches(
                request.password(),
                customer.getPassword())) {

            throw new InvalidCredentialsException();
        }

        String token = tokenService.generateToken(customer);

        return new LoginResponse(token, customer.getId(), customer.getFirstName(), customer.getPreferredName());
    }
}
