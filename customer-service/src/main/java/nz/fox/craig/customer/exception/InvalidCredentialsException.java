package nz.fox.craig.customer.exception;

public class InvalidCredentialsException extends RuntimeException {
    public InvalidCredentialsException() {
		super("Invalid email or password");
	}

}
