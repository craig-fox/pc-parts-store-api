package nz.fox.craig.order.exception;

public class DownstreamServiceUnavailableException
        extends RuntimeException {

    public DownstreamServiceUnavailableException(
            String service,
            Throwable cause) {

        super(service + " service is unavailable", cause);
    }
}
