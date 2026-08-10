package nz.fox.craig.auth.service;

import lombok.RequiredArgsConstructor;
import nz.fox.craig.auth.client.CustomerClient;
import nz.fox.craig.auth.dto.AuthenticatedCustomer;
import nz.fox.craig.auth.dto.LoginRequest;
import nz.fox.craig.auth.dto.LoginResponse;
import nz.fox.craig.auth.exception.CustomerInactiveException;
import nz.fox.craig.auth.exception.InvalidCredentialsException;
import nz.fox.craig.dto.AuthenticatedUser;
import nz.fox.craig.dto.Role;
import nz.fox.craig.security.TokenService;

import java.util.Set;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthenticationService {

    private final CustomerClient customerClient;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;

    public LoginResponse login(LoginRequest request) {
    
        AuthenticatedCustomer customer =
                customerClient.findByEmail(request.email());
    
        if (!customer.active()) {
            throw new CustomerInactiveException();
        }
    
        boolean passwordMatches = passwordEncoder.matches(
                request.password(),
                customer.password());
    
        if (!passwordMatches) {
            throw new InvalidCredentialsException();
        }
    
        AuthenticatedUser user = new AuthenticatedUser(
                customer.id(),
                customer.email(),
                Set.of(Role.ROLE_CUSTOMER)
        );
    
        String token = tokenService.generateToken(user);
    
        return new LoginResponse(
                token,
                customer.id(),
                customer.firstName(),
                customer.preferredName()
        );
    }
}
