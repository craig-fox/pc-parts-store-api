package nz.fox.craig.exception;

public class UnauthenticatedException extends RuntimeException {
    public UnauthenticatedException() {
        super("No authenticated customer");
    }
}
