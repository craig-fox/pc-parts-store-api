package nz.fox.craig.inventory.controller;


import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.security.test.context.support.WithMockUser;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.UUID;

import nz.fox.craig.inventory.config.SecurityConfig;
import nz.fox.craig.inventory.dto.InventoryReservationRequest;
import nz.fox.craig.inventory.dto.InventoryResponse;
import nz.fox.craig.inventory.exception.InventoryExceptionHandler;
import nz.fox.craig.inventory.exception.InventoryNotFoundException;
import nz.fox.craig.inventory.model.InventoryStatus;
import nz.fox.craig.inventory.service.InventoryService;
import nz.fox.craig.security.TokenService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(InventoryController.class)
@Import({
        InventoryExceptionHandler.class,
        SecurityConfig.class
})
class InventoryControllerTest {

    @Autowired 
    private MockMvc mockMvc;

    @Autowired 
    private ObjectMapper objectMapper;

    @MockitoBean 
    private InventoryService inventoryService;

    @MockitoBean
    private TokenService tokenService;

    private UUID productId;
    private InventoryResponse response;

    @BeforeEach
    void setUp() {
        productId = UUID.randomUUID();

        response =
                new InventoryResponse(
                        productId,
                        20,
                        5,
                        15,
                        InventoryStatus.IN_STOCK,
                        LocalDateTime.of(2026, 7, 30, 10, 0));
    }

    @Test
    @WithMockUser
    void shouldReturnInventory() throws Exception {

        when(inventoryService.getInventory(productId)).thenReturn(response);

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
    @WithMockUser
    void shouldReturnNotFoundWhenInventoryMissing() throws Exception {

        when(inventoryService.getInventory(productId))
                .thenThrow(new InventoryNotFoundException(productId));

        mockMvc.perform(get("/api/inventory/{productId}", productId))
                .andExpect(status().isNotFound());

        verify(inventoryService).getInventory(productId);
    }

    @Test
    @WithMockUser
    void shouldReserveStock() throws Exception {

        InventoryReservationRequest request = new InventoryReservationRequest(3);

        when(inventoryService.reserveStock(productId, 3)).thenReturn(response);

        mockMvc.perform(
                        post("/api/inventory/{productId}/reserve", productId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.availableQuantity").value(15));

        verify(inventoryService).reserveStock(productId, 3);
        verifyNoMoreInteractions(inventoryService);
    }

    @Test
    @WithMockUser
    void shouldReturnBadRequestWhenQuantityInvalid() throws Exception {

        InventoryReservationRequest request = new InventoryReservationRequest(0);

        mockMvc.perform(
                        post("/api/inventory/{productId}/reserve", productId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(inventoryService);
    }

    @Test
    @WithMockUser
    void shouldReleaseReservation() throws Exception {

        InventoryReservationRequest request = new InventoryReservationRequest(3);

        when(inventoryService.releaseReservation(productId, 3)).thenReturn(response);

        mockMvc.perform(
                        post("/api/inventory/{productId}/release", productId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.availableQuantity").value(15));

        verify(inventoryService).releaseReservation(productId, 3);
        verifyNoMoreInteractions(inventoryService);
    }

    @Test
    @WithMockUser
    void shouldReturnBadRequestWhenReleaseInvalid() throws Exception {

        InventoryReservationRequest request = new InventoryReservationRequest(0);

        mockMvc.perform(
                        post("/api/inventory/{productId}/release", productId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(inventoryService);
    }

    @Test
    @WithMockUser
    void shouldConfirmReservation() throws Exception {

        InventoryReservationRequest request = new InventoryReservationRequest(3);

        when(inventoryService.confirmReservation(productId, 3)).thenReturn(response);

        mockMvc.perform(
                        post("/api/inventory/{productId}/confirm", productId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.availableQuantity").value(15));

        verify(inventoryService).confirmReservation(productId, 3);
        verifyNoMoreInteractions(inventoryService);
    }

    @Test
    @WithMockUser
    void shouldReturnBadRequestWhenConfirmInvalid() throws Exception {

        InventoryReservationRequest request = new InventoryReservationRequest(0);

        mockMvc.perform(
                        post("/api/inventory/{productId}/confirm", productId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(inventoryService);
    }

    @Test
    void shouldRequireAuthentication() throws Exception {

        mockMvc.perform(get("/api/inventory/{productId}", productId))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(inventoryService);
    }

    @Test
    void shouldRequireAuthenticationWhenReservingStock() throws Exception {

        InventoryReservationRequest request = new InventoryReservationRequest(3);

        mockMvc.perform(
                        post("/api/inventory/{productId}/reserve", productId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(inventoryService);
    }
}
