package nz.fox.craig.order.exception;

public class IdempotencyKeyReuseException extends RuntimeException {

    public IdempotencyKeyReuseException(String idempotencyKey) {
        super("Idempotency key has already been used with a different request: " + idempotencyKey);
    }
}
