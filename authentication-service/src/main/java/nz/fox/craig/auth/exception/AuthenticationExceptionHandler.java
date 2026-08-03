package nz.fox.craig.auth.exception;

import java.time.Instant;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import jakarta.servlet.http.HttpServletRequest;

@RestControllerAdvice
public class AuthenticationExceptionHandler {

    @ExceptionHandler(InvalidCredentialsException.class)
	public ResponseEntity<ApiError> handleInvalidCredentials(
			InvalidCredentialsException ex,
			HttpServletRequest request) {

		return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
				.body(new ApiError(
						Instant.now(),
						HttpStatus.UNAUTHORIZED.value(),
						HttpStatus.UNAUTHORIZED.getReasonPhrase(),
						ex.getMessage(),
						Map.of(),
						request.getRequestURI()));
	}

    @ExceptionHandler(CustomerInactiveException.class)
	public ResponseEntity<ApiError> handleCustomerInactive(
			CustomerInactiveException ex,
			HttpServletRequest request) {

		return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
				.body(new ApiError(
						Instant.now(),
						HttpStatus.UNAUTHORIZED.value(),
						HttpStatus.UNAUTHORIZED.getReasonPhrase(),
						ex.getMessage(),
						Map.of(),
						request.getRequestURI()));
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ApiError> handleValidationException(
			MethodArgumentNotValidException ex,
			HttpServletRequest request) {

		Map<String, String> validationErrors =
				ex.getBindingResult()
						.getFieldErrors()
						.stream()
						.collect(Collectors.toMap(
								FieldError::getField,
								FieldError::getDefaultMessage,
								(first, second) -> first));

		String message = validationErrors.entrySet()
				.stream()
				.findFirst()
				.map(entry -> entry.getKey() + ": " + entry.getValue())
				.orElse("Validation failed");

		return ResponseEntity.badRequest()
				.body(new ApiError(
						Instant.now(),
						HttpStatus.BAD_REQUEST.value(),
						HttpStatus.BAD_REQUEST.getReasonPhrase(),
						message,
						validationErrors,
						request.getRequestURI()));
	}
}
