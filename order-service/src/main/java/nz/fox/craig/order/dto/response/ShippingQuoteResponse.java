package nz.fox.craig.order.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import lombok.Builder;
import nz.fox.craig.api.ShippingMethod;

@Builder
public record ShippingQuoteResponse(
    UUID id,
    UUID orderId,
    ShippingAddressResponse destination,
    BigDecimal weightKg,
    ShippingMethod shippingMethod,
    BigDecimal price,
    String currency,
    int estimatedDeliveryMin,
    int estimatedDeliveryMax,
    LocalDateTime expiresAt,
    LocalDateTime createdAt) {
}
