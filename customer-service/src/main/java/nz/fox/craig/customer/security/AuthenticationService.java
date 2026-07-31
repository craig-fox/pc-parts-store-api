package nz.fox.craig.customer.security;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import nz.fox.craig.customer.dto.LoginRequest;
import nz.fox.craig.customer.dto.LoginResponse;
import nz.fox.craig.customer.exception.InvalidCredentialsException;
import nz.fox.craig.customer.model.Customer;
import nz.fox.craig.customer.model.CustomerStatus;
import nz.fox.craig.customer.repository.CustomerRepository;

@Service
@RequiredArgsConstructor
public class AuthenticationService {

    private final CustomerRepository customerRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public LoginResponse login(LoginRequest request) {

        final Customer customer = customerRepository.findByEmail(request.email())
                .orElseThrow(() ->
                        new InvalidCredentialsException());

        if (customer.getStatus() != CustomerStatus.ACTIVE) {
            throw new InvalidCredentialsException();
        }

        if (!passwordEncoder.matches(
                request.password(),
                customer.getPassword())) {

            throw new InvalidCredentialsException();
        }

        final String token = jwtService.generateToken(customer);

        return new LoginResponse(token, customer.getId(), customer.getFirstName(), customer.getPreferredName());
    }
}
