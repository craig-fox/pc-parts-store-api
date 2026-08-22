package nz.fox.craig.order.client;

import java.math.BigDecimal;
import java.util.UUID;

public interface PaymentClient {

    void processPayment(
            UUID orderId,
            UUID customerId,
            BigDecimal amount,
            String currency);
}
