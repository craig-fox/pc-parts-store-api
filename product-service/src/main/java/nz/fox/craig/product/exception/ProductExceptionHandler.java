package nz.fox.craig.product.exception;

import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.Map;
import nz.fox.craig.api.ApiError;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ProductExceptionHandler {

    @ExceptionHandler(ProductNotFoundException.class)
    public ResponseEntity<ApiError> handleProductNotFound(
            ProductNotFoundException ex, HttpServletRequest request) {

        ApiError error =
                new ApiError(
                        Instant.now(),
                        HttpStatus.NOT_FOUND.value(),
                        HttpStatus.NOT_FOUND.getReasonPhrase(),
                        ex.getMessage(),
                        Map.of(),
                        request.getRequestURI());

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }
}
