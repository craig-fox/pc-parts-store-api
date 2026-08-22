package nz.fox.craig.order.dto.request;

import java.math.BigDecimal;
import java.util.UUID;

public record PaymentRequest(
    UUID orderId,
    UUID customerId,
    BigDecimal amount,
    String currency) {}
