package nz.fox.craig.auth.client;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import lombok.RequiredArgsConstructor;
import nz.fox.craig.auth.dto.AuthenticatedCustomer;

@Component
@RequiredArgsConstructor
public class CustomerClient {

    private final RestClient restClient;

    public AuthenticatedCustomer findByEmail(String email) {
        throw new UnsupportedOperationException("Not implemented");
    }
}