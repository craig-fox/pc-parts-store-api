package nz.fox.craig.shipping.service;

import nz.fox.craig.shipping.model.ShippingMethod;

import java.math.BigDecimal;

record ShippingRateTier(
        BigDecimal maximumWeightKg,
        BigDecimal standardPrice,
        BigDecimal expressPrice,
        int standardMinDays,
        int standardMaxDays,
        int expressMinDays,
        int expressMaxDays) {

    boolean supports(BigDecimal weightKg) {
        return maximumWeightKg == null
                || weightKg.compareTo(maximumWeightKg) <= 0;
    }

    ShippingRate rateFor(ShippingMethod shippingMethod) {
        return switch (shippingMethod) {
            case STANDARD -> new ShippingRate(
                    standardPrice,
                    standardMinDays,
                    standardMaxDays
            );
            case EXPRESS -> new ShippingRate(
                    expressPrice,
                    expressMinDays,
                    expressMaxDays
            );
        };
    }
}
