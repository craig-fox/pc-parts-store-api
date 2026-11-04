package nz.fox.craig.messaging;

public final class EventTypes {

    public static final String ORDER_PLACED = "OrderPlaced";

    public static final String PAYMENT_COMPLETED = "PaymentCompleted";

    public static final String PAYMENT_FAILED = "PaymentFailed";

    public static final String SHIPMENT_DISPATCHED = "ShipmentDispatched";

    public static final String SHIPMENT_DELIVERED = "ShipmentDelivered";

    private EventTypes() {
    }
}
