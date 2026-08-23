package nz.fox.craig.shipping.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import nz.fox.craig.shipping.model.ShipmentStatus;
import nz.fox.craig.shipping.model.ShippingMethod;

public record ShipmentResponse(
    UUID id,
    UUID orderId,
    ShippingAddressResponse destination,
    BigDecimal weightKg,
    ShippingMethod shippingMethod,
    BigDecimal shippingCost,
    String currency,
    ShipmentStatus status,
    String trackingNumber,
    LocalDateTime createdAt,
    LocalDateTime updatedAt) {
}
