package nz.fox.craig.security.exception;

public class UnauthenticatedException extends RuntimeException {
    public UnauthenticatedException() {
        super("No authenticated customer");
    }
}
