package nz.fox.craig.inventory.controller;

import java.time.LocalDateTime;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;

import nz.fox.craig.inventory.dto.InventoryReservationRequest;
import nz.fox.craig.inventory.dto.InventoryResponse;
import nz.fox.craig.inventory.exception.InventoryExceptionHandler;
import nz.fox.craig.inventory.exception.InventoryNotFoundException;
import nz.fox.craig.inventory.model.InventoryStatus;
import nz.fox.craig.inventory.service.InventoryService;

import static org.mockito.Mockito.*;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@WebMvcTest(InventoryController.class)
@Import(InventoryExceptionHandler.class)
class InventoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private InventoryService inventoryService;

    private UUID productId;
    private InventoryResponse response;

    @BeforeEach
    void setUp() {
        productId = UUID.randomUUID();

        response = new InventoryResponse(
                productId,
                20,
                5,
                15,
                InventoryStatus.IN_STOCK,
                LocalDateTime.of(2026, 7, 30, 10, 0));
    }

    @Test
    void shouldReturnInventory() throws Exception {

        when(inventoryService.getInventory(productId))
                .thenReturn(response);

        mockMvc.perform(get("/api/inventory/{productId}", productId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId").value(productId.toString()))
                .andExpect(jsonPath("$.quantityOnHand").value(20))
                .andExpect(jsonPath("$.quantityReserved").value(5))
                .andExpect(jsonPath("$.availableQuantity").value(15))
                .andExpect(jsonPath("$.status").value("IN_STOCK"));

        verify(inventoryService).getInventory(productId);
        verifyNoMoreInteractions(inventoryService);
    }

    @Test
    void shouldReturnNotFoundWhenInventoryMissing() throws Exception {

        when(inventoryService.getInventory(productId))
                .thenThrow(new InventoryNotFoundException(productId));

        mockMvc.perform(get("/api/inventory/{productId}", productId))
                .andExpect(status().isNotFound());

        verify(inventoryService).getInventory(productId);
    }

    @Test
    void shouldReserveStock() throws Exception {

        InventoryReservationRequest request = new InventoryReservationRequest(3);

        when(inventoryService.reserveStock(productId, 3))
                .thenReturn(response);

        mockMvc.perform(post("/api/inventory/{productId}/reserve", productId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.availableQuantity").value(15));

        verify(inventoryService).reserveStock(productId, 3);
        verifyNoMoreInteractions(inventoryService);
    }

    @Test
    void shouldReturnBadRequestWhenQuantityInvalid() throws Exception {

        InventoryReservationRequest request = new InventoryReservationRequest(0);

        mockMvc.perform(post("/api/inventory/{productId}/reserve", productId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(inventoryService);
    }

    @Test
    void shouldReleaseReservation() throws Exception {

        InventoryReservationRequest request = new InventoryReservationRequest(3);

        when(inventoryService.releaseReservation(productId, 3))
                .thenReturn(response);

        mockMvc.perform(post("/api/inventory/{productId}/release", productId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.availableQuantity").value(15));

        verify(inventoryService).releaseReservation(productId, 3);
        verifyNoMoreInteractions(inventoryService);
    }

    @Test
    void shouldReturnBadRequestWhenReleaseInvalid() throws Exception {

        InventoryReservationRequest request = new InventoryReservationRequest(0);

        mockMvc.perform(post("/api/inventory/{productId}/release", productId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(inventoryService);
    }


    @Test
    void shouldConfirmReservation() throws Exception {

        InventoryReservationRequest request = new InventoryReservationRequest(3);

        when(inventoryService.confirmReservation(productId, 3))
                .thenReturn(response);

        mockMvc.perform(post("/api/inventory/{productId}/confirm", productId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.availableQuantity").value(15));

        verify(inventoryService).confirmReservation(productId, 3);
        verifyNoMoreInteractions(inventoryService);
    }

    @Test
    void shouldReturnBadRequestWhenConfirmInvalid() throws Exception {

        InventoryReservationRequest request = new InventoryReservationRequest(0);

        mockMvc.perform(post("/api/inventory/{productId}/confirm", productId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(inventoryService);
    }

}
