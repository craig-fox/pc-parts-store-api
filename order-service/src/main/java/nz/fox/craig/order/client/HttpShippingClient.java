package nz.fox.craig.order.client;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import io.github.resilience4j.retry.annotation.Retry;
import nz.fox.craig.order.dto.request.ShippingQuoteRequest;
import nz.fox.craig.order.dto.response.ShippingQuoteResponse;
import nz.fox.craig.order.exception.DownstreamServiceUnavailableException;

@Component
public class HttpShippingClient implements ShippingClient {

    private final RestClient restClient;

    public HttpShippingClient(
            @Qualifier("shippingRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    @Retry(name = "downstreamRead")
    @Override
    public ShippingQuoteResponse calculateQuote(ShippingQuoteRequest request) {
        try {
            return restClient.post()
                    .uri("/api/shipping/quotes")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(ShippingQuoteResponse.class);
        } catch (HttpServerErrorException | ResourceAccessException ex) {
            throw new DownstreamServiceUnavailableException(
                    "Shipping",
                    ex);
        }
    }
}
