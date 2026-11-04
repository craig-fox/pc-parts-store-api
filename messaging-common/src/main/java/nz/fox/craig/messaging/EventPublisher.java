package nz.fox.craig.messaging;

import java.util.concurrent.CompletionStage;

public interface EventPublisher {

    CompletionStage<Void> publish(EventEnvelope<?> event);
}
