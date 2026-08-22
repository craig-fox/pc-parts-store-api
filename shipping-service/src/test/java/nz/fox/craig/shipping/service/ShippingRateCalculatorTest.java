package nz.fox.craig.shipping.service;

import nz.fox.craig.shipping.model.ShippingMethod;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.math.BigDecimal;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ShippingRateCalculatorTest {

    private final ShippingRateCalculator calculator =
            new ShippingRateCalculator();

    @ParameterizedTest
    @MethodSource("standardRates")
    void shouldCalculateStandardRate(
            String weight,
            String expectedPrice,
            int expectedMinDays,
            int expectedMaxDays) {

        ShippingRate rate = calculator.calculate(
                new BigDecimal(weight),
                ShippingMethod.STANDARD
        );

        assertThat(rate.price())
                .isEqualByComparingTo(expectedPrice);
        assertThat(rate.estimatedDeliveryMin())
                .isEqualTo(expectedMinDays);
        assertThat(rate.estimatedDeliveryMax())
                .isEqualTo(expectedMaxDays);
    }

    private static Stream<Arguments> standardRates() {
        return Stream.of(
                Arguments.of("0.5", "8.00", 2, 5),
                Arguments.of("0.501", "15.00", 2, 5),
                Arguments.of("2.0", "15.00", 2, 5),
                Arguments.of("2.001", "25.00", 2, 5),
                Arguments.of("5.0", "25.00", 2, 5),
                Arguments.of("5.001", "35.00", 3, 6),
                Arguments.of("10.0", "35.00", 3, 6),
                Arguments.of("10.001", "50.00", 3, 7)
        );
    }

    @ParameterizedTest
    @MethodSource("expressRates")
    void shouldCalculateExpressRate(
            String weight,
            String expectedPrice,
            int expectedMinDays,
            int expectedMaxDays) {

        ShippingRate rate = calculator.calculate(
                new BigDecimal(weight),
                ShippingMethod.EXPRESS
        );

        assertThat(rate.price())
                .isEqualByComparingTo(expectedPrice);
        assertThat(rate.estimatedDeliveryMin())
                .isEqualTo(expectedMinDays);
        assertThat(rate.estimatedDeliveryMax())
                .isEqualTo(expectedMaxDays);
    }

    private static Stream<Arguments> expressRates() {
        return Stream.of(
                Arguments.of("0.5", "15.00", 1, 2),
                Arguments.of("0.501", "25.00", 1, 2),
                Arguments.of("2.0", "25.00", 1, 2),
                Arguments.of("2.001", "40.00", 1, 2),
                Arguments.of("5.0", "40.00", 1, 2),
                Arguments.of("5.001", "55.00", 1, 3),
                Arguments.of("10.0", "55.00", 1, 3),
                Arguments.of("10.001", "75.00", 1, 3)
        );
    }

    @Test
    void shouldRejectZeroWeight() {
        assertThatThrownBy(() ->
                calculator.calculate(
                        new BigDecimal("0"),
                        ShippingMethod.STANDARD
                ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Weight must be greater than zero");
    }

    @Test
    void shouldRejectNegativeWeight() {
        assertThatThrownBy(() ->
                calculator.calculate(
                        new BigDecimal("-1"),
                        ShippingMethod.STANDARD
                ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Weight must be greater than zero");
    }

    @Test
    void shouldRejectNullWeight() {
        assertThatThrownBy(() ->
                calculator.calculate(
                        null,
                        ShippingMethod.STANDARD
                ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Weight must be greater than zero");
    }
}
