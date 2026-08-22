package nz.fox.craig.shipping.service;

import java.math.BigDecimal;

public record ShippingRate(
        BigDecimal price,
        int estimatedDeliveryMin,
        int estimatedDeliveryMax) {
}
