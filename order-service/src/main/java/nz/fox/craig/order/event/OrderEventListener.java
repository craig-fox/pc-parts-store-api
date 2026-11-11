package nz.fox.craig.order.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nz.fox.craig.messaging.EventEnvelope;
import nz.fox.craig.messaging.EventPublisher;
import org.springframework.amqp.AmqpException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderEventListener {

    private final EventPublisher eventPublisher;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleOrderPlaced(OrderPlacedEvent event) {
        try {
            eventPublisher
                    .publish(EventEnvelope.of(event, event.orderId().toString()))
                    .whenComplete(
                            (ignored, error) -> {
                                if (error != null) {
                                    log.error(
                                            "Failed to publish ORDER_PLACED event for order {}",
                                            event.orderId(),
                                            error);
                                } else {
                                    log.info(
                                            "Published ORDER_PLACED event for order {}",
                                            event.orderId());
                                }
                            });
        } catch (AmqpException exception) {
            log.error(
                    "Unable to initiate publication of ORDER_PLACED event for order {}",
                    event.orderId(),
                    exception);
        }
    }
}
