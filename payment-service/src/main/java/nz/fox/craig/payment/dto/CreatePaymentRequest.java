package nz.fox.craig.payment.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.UUID;

public record CreatePaymentRequest(
    @NotNull(message = "Order id is required")
    UUID orderId,

    @NotNull(message = "Customer id is required")
    UUID customerId,

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than zero")
    BigDecimal amount,

    @NotBlank(message = "Currency is required")
    String currency) {
}
