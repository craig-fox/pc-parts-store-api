package nz.fox.craig.customer.exception;

import java.time.Instant;
import java.util.HashMap;
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
public class CustomerExceptionHandler {

	@ExceptionHandler(CustomerNotFoundException.class)
	public ResponseEntity<ApiError> handleCustomerNotFound(CustomerNotFoundException ex, HttpServletRequest request) {
		Map<String, String> validationErrors = new HashMap<>();
		validationErrors.put("message", ex.getMessage());
		return ResponseEntity.status(HttpStatus.NOT_FOUND)
        .body(new ApiError(
                Instant.now(),
                HttpStatus.NOT_FOUND.value(),
                HttpStatus.NOT_FOUND.getReasonPhrase(),
                validationErrors,
                request.getRequestURI()
        ));
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ApiError> handleValidationErrors(MethodArgumentNotValidException ex, HttpServletRequest request) {
	
		Map<String, String> validationErrors =
		ex.getBindingResult()
			.getFieldErrors()
			.stream()
			.collect(Collectors.toMap(
					FieldError::getField,
					FieldError::getDefaultMessage,
					(first, second) -> first
			));
		return ResponseEntity.status(HttpStatus.BAD_REQUEST)
		.body(new ApiError(
			Instant.now(),
			HttpStatus.BAD_REQUEST.value(),
			HttpStatus.BAD_REQUEST.getReasonPhrase(),
			validationErrors,
			request.getRequestURI()
		));
	}

}
