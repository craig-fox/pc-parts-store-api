package nz.fox.craig.messaging;

public interface DomainEvent {

    String eventType();

    int eventVersion();
}
