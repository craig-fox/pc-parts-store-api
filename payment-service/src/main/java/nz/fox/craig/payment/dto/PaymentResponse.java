package nz.fox.craig.payment.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import nz.fox.craig.payment.model.PaymentStatus;

public record PaymentResponse(
    UUID id,
    UUID orderId,
    UUID customerId,
    BigDecimal amount,
    String currency,
    PaymentStatus status,
    Instant createdAt,
    Instant updatedAt) {
}