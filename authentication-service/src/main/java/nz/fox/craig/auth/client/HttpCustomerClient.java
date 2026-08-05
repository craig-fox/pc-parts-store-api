package nz.fox.craig.auth.client;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import lombok.RequiredArgsConstructor;
import nz.fox.craig.auth.dto.AuthenticatedCustomer;

@Component
@RequiredArgsConstructor
public class HttpCustomerClient implements CustomerClient {

    private final RestClient restClient;

    @Override
    public AuthenticatedCustomer findByEmail(String email) {
        return restClient.get()
                .uri("/api/customers/email/{email}", email)
                .retrieve()
                .body(AuthenticatedCustomer.class);
    }
}
