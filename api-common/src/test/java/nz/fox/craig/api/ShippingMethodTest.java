package nz.fox.craig.api;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ShippingMethodTest {

    @Test
    void shouldContainStandardAndExpress() {
        assertThat(ShippingMethod.values())
                .containsExactly(
                        ShippingMethod.STANDARD,
                        ShippingMethod.EXPRESS);
    }

    @Test
    void shouldResolveShippingMethodsByName() {
        assertThat(ShippingMethod.valueOf("STANDARD"))
                .isEqualTo(ShippingMethod.STANDARD);

        assertThat(ShippingMethod.valueOf("EXPRESS"))
                .isEqualTo(ShippingMethod.EXPRESS);
    }
}
