package nz.fox.craig.customer.exception;

import java.time.Instant;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import jakarta.servlet.http.HttpServletRequest;

@RestControllerAdvice
public class CustomerExceptionHandler {

	@ExceptionHandler(CustomerNotFoundException.class)
	public ResponseEntity<ApiError> handleCustomerNotFound(CustomerNotFoundException ex, HttpServletRequest request) {
		String message = ex.getMessage();
		return ResponseEntity.status(HttpStatus.NOT_FOUND)
				.body(new ApiError(
						Instant.now(),
						HttpStatus.NOT_FOUND.value(),
						HttpStatus.NOT_FOUND.getReasonPhrase(),
						message,
						Map.of(),
						request.getRequestURI()));
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ApiError> handleValidationErrors(MethodArgumentNotValidException ex,
			HttpServletRequest request) {

		Map<String, String> validationErrors = ex.getBindingResult()
				.getFieldErrors()
				.stream()
				.collect(Collectors.toMap(
						FieldError::getField,
						FieldError::getDefaultMessage,
						(first, second) -> first));
		return ResponseEntity.status(HttpStatus.BAD_REQUEST)
				.body(new ApiError(
						Instant.now(),
						HttpStatus.BAD_REQUEST.value(),
						HttpStatus.BAD_REQUEST.getReasonPhrase(),
						"Validation failed",
						validationErrors,
						request.getRequestURI()));
	}

	@ExceptionHandler(CustomerAlreadyExistsException.class)
	public ResponseEntity<ApiError> handleCustomerAlreadyExists(
			CustomerAlreadyExistsException ex,
			HttpServletRequest request) {

		return ResponseEntity.status(HttpStatus.CONFLICT)
				.body(new ApiError(
						Instant.now(),
						HttpStatus.CONFLICT.value(),
						HttpStatus.CONFLICT.getReasonPhrase(),
						ex.getMessage(),
						Map.of(),
						request.getRequestURI()));
	}

	@ExceptionHandler(CustomerInactiveException.class)
	public ResponseEntity<ApiError> handleCustomerInactive(
			CustomerInactiveException ex,
			HttpServletRequest request) {

		return ResponseEntity.status(HttpStatus.FORBIDDEN)
				.body(new ApiError(
						Instant.now(),
						HttpStatus.FORBIDDEN.value(),
						HttpStatus.FORBIDDEN.getReasonPhrase(),
						ex.getMessage(),
						Map.of(),
						request.getRequestURI()));
	}

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

	@ExceptionHandler(AccessDeniedException.class)
	public ResponseEntity<ApiError> handleAccessDenied(
			AccessDeniedException ex,
			HttpServletRequest request) {

		return ResponseEntity.status(HttpStatus.FORBIDDEN)
				.body(new ApiError(
						Instant.now(),
						HttpStatus.FORBIDDEN.value(),
						HttpStatus.FORBIDDEN.getReasonPhrase(),
						ex.getMessage(),
						Map.of(),
						request.getRequestURI()));
	}

}
