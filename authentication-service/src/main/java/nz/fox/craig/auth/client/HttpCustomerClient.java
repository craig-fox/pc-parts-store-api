package nz.fox.craig.auth.client;

import nz.fox.craig.api.DownstreamServiceUnavailableException;
import nz.fox.craig.auth.dto.AuthenticatedCustomer;
import nz.fox.craig.auth.exception.InvalidCredentialsException;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

@Component
public class HttpCustomerClient implements CustomerClient {

    private final RestClient restClient;

    public HttpCustomerClient(
            @Qualifier("customerRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    @Override
    public AuthenticatedCustomer findByEmail(String email) {
        try {
            return restClient
                    .get()
                    .uri("/api/customers/email/{email}", email)
                    .retrieve()
                    .body(AuthenticatedCustomer.class);
        } catch (HttpClientErrorException.NotFound ex) {
            throw new InvalidCredentialsException();
        } catch (HttpServerErrorException | ResourceAccessException ex) {
            throw new DownstreamServiceUnavailableException("Customer", ex);
        }
    }
}
