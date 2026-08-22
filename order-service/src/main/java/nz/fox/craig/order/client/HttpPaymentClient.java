package nz.fox.craig.order.client;

import java.math.BigDecimal;
import java.util.UUID;
import nz.fox.craig.order.dto.request.PaymentRequest;


import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class HttpPaymentClient implements PaymentClient {

    private final RestClient restClient;


    public HttpPaymentClient(@Qualifier("paymentRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    @Override
    public void processPayment(
            UUID orderId,
            UUID customerId,
            BigDecimal amount,
            String currency) {

        restClient.post()
                .uri("/api/payments")
                .body(new PaymentRequest(orderId, customerId, amount, currency))
                .retrieve()
                .toBodilessEntity();
    }
}