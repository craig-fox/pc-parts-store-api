package nz.fox.craig.shipping.service;

import org.springframework.stereotype.Component;

import nz.fox.craig.api.ShippingMethod;

import java.math.BigDecimal;
import java.util.List;

@Component
public class ShippingRateCalculator {

    private static final List<ShippingRateTier> RATE_TIERS = List.of(
            new ShippingRateTier(
                    new BigDecimal("0.5"),
                    new BigDecimal("8.00"),
                    new BigDecimal("15.00"),
                    2,
                    5,
                    1,
                    2
            ),
            new ShippingRateTier(
                    new BigDecimal("2.0"),
                    new BigDecimal("15.00"),
                    new BigDecimal("25.00"),
                    2,
                    5,
                    1,
                    2
            ),
            new ShippingRateTier(
                    new BigDecimal("5.0"),
                    new BigDecimal("25.00"),
                    new BigDecimal("40.00"),
                    2,
                    5,
                    1,
                    2
            ),
            new ShippingRateTier(
                    new BigDecimal("10.0"),
                    new BigDecimal("35.00"),
                    new BigDecimal("55.00"),
                    3,
                    6,
                    1,
                    3
            ),
            new ShippingRateTier(
                    null,
                    new BigDecimal("50.00"),
                    new BigDecimal("75.00"),
                    3,
                    7,
                    1,
                    3
            )
    );

    public ShippingRate calculate(
            BigDecimal weightKg,
            ShippingMethod shippingMethod) {

        if (weightKg == null || weightKg.signum() <= 0) {
            throw new IllegalArgumentException("Weight must be greater than zero");
        }

        ShippingRateTier tier = RATE_TIERS.stream()
                .filter(rateTier -> rateTier.supports(weightKg))
                .findFirst()
                .orElseThrow();

        return tier.rateFor(shippingMethod);
    }
}
