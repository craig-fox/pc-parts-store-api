package nz.fox.craig.inventory.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.UUID;
import nz.fox.craig.inventory.dto.InventoryReservationRequest;
import nz.fox.craig.inventory.dto.InventoryResponse;
import nz.fox.craig.inventory.exception.InventoryExceptionHandler;
import nz.fox.craig.inventory.exception.InventoryNotFoundException;
import nz.fox.craig.inventory.model.InventoryStatus;
import nz.fox.craig.inventory.service.InventoryService;
import nz.fox.craig.security.TokenService;
import tools.jackson.databind.json.JsonMapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(InventoryController.class)
@Import({
    InventoryExceptionHandler.class,
    InventoryControllerTest.TestSecurityConfiguration.class
})
class InventoryControllerTest {

    @Autowired private MockMvc mockMvc;

    @Autowired private JsonMapper objectMapper;

    @MockitoBean private InventoryService inventoryService;

    @MockitoBean private TokenService tokenService;

    private UUID productId;
    private InventoryResponse response;

    @TestConfiguration
    @EnableWebSecurity
    static class TestSecurityConfiguration {
    
        @Bean
        SecurityFilterChain testSecurityFilterChain(HttpSecurity http)
                throws Exception {
            return http
                    .csrf(csrf -> csrf.disable())
                    .authorizeHttpRequests(auth -> auth
                            .anyRequest().authenticated())
                    .httpBasic(basic -> { })
                    .build();
        }
    }

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
    void shouldReturnInventory() throws Exception {
        when(inventoryService.getInventory(productId)).thenReturn(response);

        mockMvc.perform(
                get("/api/inventory/{productId}", productId)
                    .with(user("test-user")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.productId").value(productId.toString()));

        verify(inventoryService).getInventory(productId);
    }

    @Test
    @WithMockUser
    void shouldReturnNotFoundWhenInventoryMissing() throws Exception {

        when(inventoryService.getInventory(productId))
                .thenThrow(new InventoryNotFoundException(productId));

        mockMvc.perform(
            get("/api/inventory/{productId}", productId)
                .with(user("test-user")))
                .andExpect(status().isNotFound());

        verify(inventoryService).getInventory(productId);
    }


    @Test
    void shouldReserveStock() throws Exception {
        InventoryReservationRequest request =
                new InventoryReservationRequest(3);

        when(inventoryService.reserveStock(productId, 3))
                .thenReturn(response);

        mockMvc.perform(
                        post("/api/inventory/{productId}/reserve", productId)
                                .with(user("test-user"))
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
                                .with(user("test-user"))
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
                            .with(user("test-user"))
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
                            .with(user("test-user"))
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
                            .with(user("test-user"))
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
                            .with(user("test-user"))
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
