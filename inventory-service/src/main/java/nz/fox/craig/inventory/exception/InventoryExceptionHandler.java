package nz.fox.craig.inventory.exception;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class InventoryExceptionHandler {
    @ExceptionHandler(InventoryNotFoundException.class)
	public ResponseEntity<Map<String, String>> handleInventoryNotFound(InventoryNotFoundException ex) {
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", ex.getMessage()));
	}
}
