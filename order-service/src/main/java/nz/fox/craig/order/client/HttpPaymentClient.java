package nz.fox.craig.order.client;

import java.math.BigDecimal;
import java.util.UUID;
import nz.fox.craig.order.dto.request.PaymentRequest;
import nz.fox.craig.order.exception.DownstreamServiceUnavailableException;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import io.github.resilience4j.retry.annotation.Retry;

@Component
public class HttpPaymentClient implements PaymentClient {

    private final RestClient restClient;

    public HttpPaymentClient(
            @Qualifier("paymentRestClient") RestClient restClient) {

        this.restClient = restClient;
    }

    @Retry(name = "payment")
    @Override
    public void processPayment(
            UUID orderId,
            UUID customerId,
            BigDecimal amount,
            String currency) {

        try {
            restClient.post()
                    .uri("/api/payments")
                    .body(new PaymentRequest(
                            orderId,
                            customerId,
                            amount,
                            currency))
                    .retrieve()
                    .toBodilessEntity();

        } catch (HttpServerErrorException | ResourceAccessException ex) {
            throw new DownstreamServiceUnavailableException(
                    "Payment",
                    ex);
        }
    }
}