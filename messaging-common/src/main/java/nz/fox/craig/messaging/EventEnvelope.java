package nz.fox.craig.messaging;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record EventEnvelope<T>(
        UUID eventId,
        String eventType,
        int eventVersion,
        Instant occurredAt,
        String correlationId,
        T payload
) {

    public EventEnvelope {
        Objects.requireNonNull(eventId, "eventId must not be null");
        Objects.requireNonNull(eventType, "eventType must not be null");
        Objects.requireNonNull(occurredAt, "occurredAt must not be null");
        Objects.requireNonNull(payload, "payload must not be null");

        if (eventType.isBlank()) {
            throw new IllegalArgumentException(
                    "eventType must not be blank"
            );
        }

        if (eventVersion < 1) {
            throw new IllegalArgumentException(
                    "eventVersion must be greater than zero"
            );
        }
    }

    public static <T extends DomainEvent> EventEnvelope<T> of(
            T event,
            String correlationId
    ) {
        Objects.requireNonNull(event, "event must not be null");

        return new EventEnvelope<>(
                UUID.randomUUID(),
                event.eventType(),
                event.eventVersion(),
                Instant.now(),
                correlationId,
                event
        );
    }
}
