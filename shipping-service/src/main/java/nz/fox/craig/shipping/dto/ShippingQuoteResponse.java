package nz.fox.craig.shipping.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import nz.fox.craig.shipping.model.ShippingMethod;

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
