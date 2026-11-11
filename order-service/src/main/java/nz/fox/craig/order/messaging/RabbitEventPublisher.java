package nz.fox.craig.order.messaging;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nz.fox.craig.messaging.EventEnvelope;
import nz.fox.craig.messaging.EventPublisher;
import nz.fox.craig.order.config.RabbitMQConfig;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class RabbitEventPublisher implements EventPublisher {

    private final RabbitTemplate rabbitTemplate;

    @Override
    public CompletionStage<Void> publish(EventEnvelope<?> event) {
        String routingKey = toRoutingKey(event.eventType());

        try {
            rabbitTemplate.convertAndSend(RabbitMQConfig.EVENTS_EXCHANGE, routingKey, event);

            log.debug("Sent event {} with routing key {}", event.eventType(), routingKey);

            return CompletableFuture.completedFuture(null);
        } catch (AmqpException exception) {
            log.error(
                    "Failed to send event {} with routing key {}",
                    event.eventType(),
                    routingKey,
                    exception);

            return CompletableFuture.failedFuture(exception);
        }
    }

    private String toRoutingKey(String eventType) {
        return eventType.toLowerCase().replace('_', '.');
    }
}
