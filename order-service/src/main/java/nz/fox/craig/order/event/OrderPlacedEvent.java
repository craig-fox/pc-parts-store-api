package nz.fox.craig.order.event;

import java.math.BigDecimal;
import java.util.UUID;
import nz.fox.craig.messaging.DomainEvent;
import nz.fox.craig.messaging.EventTypes;

public record OrderPlacedEvent(UUID orderId, UUID customerId, BigDecimal total, String currency)
        implements DomainEvent {

    @Override
    public String eventType() {
        return EventTypes.ORDER_PLACED;
    }

    @Override
    public int eventVersion() {
        return 1;
    }
}
