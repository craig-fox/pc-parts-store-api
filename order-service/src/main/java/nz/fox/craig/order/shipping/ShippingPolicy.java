package nz.fox.craig.order.shipping;

import java.math.BigDecimal;

public final class ShippingPolicy {

    public static final BigDecimal FREE_SHIPPING_THRESHOLD = new BigDecimal("1000.00");

    public static final BigDecimal LIGHT_WEIGHT_LIMIT = new BigDecimal("0.50");

    public static final BigDecimal STANDARD_WEIGHT_LIMIT = new BigDecimal("2.00");

    public static final BigDecimal LIGHT_SHIPPING = new BigDecimal("8.00");

    public static final BigDecimal STANDARD_SHIPPING = new BigDecimal("15.00");

    public static final BigDecimal HEAVY_SHIPPING = new BigDecimal("25.00");

    private ShippingPolicy() {}
}
