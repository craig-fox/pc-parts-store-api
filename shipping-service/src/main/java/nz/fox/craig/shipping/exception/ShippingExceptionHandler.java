package nz.fox.craig.shipping.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import nz.fox.craig.api.ApiError;

import java.time.Instant;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ShippingExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidationException(
            MethodArgumentNotValidException exception,
            HttpServletRequest request) {

        String message = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .findFirst()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .orElse("Validation failed");

        return ResponseEntity.badRequest()
                .body(new ApiError(Instant.now(),
                        HttpStatus.BAD_REQUEST.value(),
                        HttpStatus.BAD_REQUEST.getReasonPhrase(),
                        message,
                        Map.of(),
                        request.getRequestURI()               
                ));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiError> handleConstraintViolationException(
            ConstraintViolationException exception,
            HttpServletRequest request) {

            return ResponseEntity.badRequest()
                .body(new ApiError(Instant.now(),
                        HttpStatus.BAD_REQUEST.value(),
                        HttpStatus.BAD_REQUEST.getReasonPhrase(),
                        exception.getMessage(),
                        Map.of(),
                        request.getRequestURI()               
                ));
    }

    @ExceptionHandler(ShippingQuoteNotFoundException.class)
    public ResponseEntity<ApiError> handleShippingQuoteNotFoundException(
            ShippingQuoteNotFoundException exception,
            HttpServletRequest request) {

            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ApiError(Instant.now(),
                        HttpStatus.NOT_FOUND.value(),
                        HttpStatus.NOT_FOUND.getReasonPhrase(),
                        exception.getMessage(),
                        Map.of(),
                        request.getRequestURI()               
                ));
    }

    @ExceptionHandler(ShipmentNotFoundException.class)
    public ResponseEntity<ApiError> handleShipmentNotFoundException(
            ShipmentNotFoundException exception,
            HttpServletRequest request) {

            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ApiError(Instant.now(),
                        HttpStatus.NOT_FOUND.value(),
                        HttpStatus.NOT_FOUND.getReasonPhrase(),
                        exception.getMessage(),
                        Map.of(),
                        request.getRequestURI()               
                ));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleException(
            Exception exception,
            HttpServletRequest request) {

            return ResponseEntity.internalServerError()
                .body(new ApiError(Instant.now(),
                        HttpStatus.INTERNAL_SERVER_ERROR.value(),
                        HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(),
                        exception.getMessage(),
                        Map.of(),
                        request.getRequestURI()               
                ));
    }
}
