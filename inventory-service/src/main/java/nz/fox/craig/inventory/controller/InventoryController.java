package nz.fox.craig.inventory.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import nz.fox.craig.inventory.dto.InventoryReservationRequest;
import nz.fox.craig.inventory.dto.InventoryResponse;
import nz.fox.craig.inventory.service.InventoryService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
@Tag(name = "Inventory", description = "Operations for managing product inventory")
public class InventoryController {

    private final InventoryService inventoryService;

    @Operation(summary = "Get inventory for a product")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Inventory found"),
        @ApiResponse(responseCode = "404", description = "Inventory not found")
    })
    @GetMapping("/{productId}")
    @ResponseStatus(HttpStatus.OK)
    public InventoryResponse getInventory(@PathVariable UUID productId) {
        return inventoryService.getInventory(productId);
    }

    @Operation(summary = "Reserve stock for a product")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Stock reserved"),
        @ApiResponse(responseCode = "400", description = "Invalid reservation request"),
        @ApiResponse(responseCode = "404", description = "Inventory not found"),
        @ApiResponse(responseCode = "409", description = "Insufficient available stock")
    })
    @PostMapping("/{productId}/reserve")
    @ResponseStatus(HttpStatus.OK)
    public InventoryResponse reserveStock(
            @PathVariable UUID productId, @Valid @RequestBody InventoryReservationRequest request) {

        return inventoryService.reserveStock(productId, request.quantity());
    }

    @Operation(summary = "Release reserved stock for a product")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Stock reservation released"),
        @ApiResponse(responseCode = "400", description = "Invalid reservation request"),
        @ApiResponse(responseCode = "404", description = "Inventory not found"),
        @ApiResponse(responseCode = "409", description = "Reservation cannot be released")
    })
    @PostMapping("/{productId}/release")
    @ResponseStatus(HttpStatus.OK)
    public InventoryResponse releaseReservation(
            @PathVariable UUID productId, @Valid @RequestBody InventoryReservationRequest request) {

        return inventoryService.releaseReservation(productId, request.quantity());
    }

    @Operation(summary = "Confirm reserved stock for a product")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Stock reservation confirmed"),
        @ApiResponse(responseCode = "400", description = "Invalid reservation request"),
        @ApiResponse(responseCode = "404", description = "Inventory not found"),
        @ApiResponse(responseCode = "409", description = "Reservation cannot be confirmed")
    })
    @PostMapping("/{productId}/confirm")
    @ResponseStatus(HttpStatus.OK)
    public InventoryResponse confirmReservation(
            @PathVariable UUID productId, @Valid @RequestBody InventoryReservationRequest request) {

        return inventoryService.confirmReservation(productId, request.quantity());
    }
}
