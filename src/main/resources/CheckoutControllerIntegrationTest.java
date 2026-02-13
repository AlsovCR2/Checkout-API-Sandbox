package Portfolio.Checkout_api_sandbox.controller;

import Portfolio.Checkout_api_sandbox.dto.request.CheckoutRequest;
import Portfolio.Checkout_api_sandbox.dto.request.CreateOrderRequest;
import Portfolio.Checkout_api_sandbox.dto.request.OrderItemRequest;
import Portfolio.Checkout_api_sandbox.dto.response.OrderResponse;
import Portfolio.Checkout_api_sandbox.TestcontainersConfiguration;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.webmvc.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests de integración para CheckoutController.
 * Verifica el flujo de checkout con Stripe (requiere configuración de Stripe).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@Transactional
class CheckoutControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private UUID orderId;
    private String idempotencyKey;

    @BeforeEach
    void setUp() throws Exception {
        // Crear una orden antes de cada test
        OrderItemRequest item = new OrderItemRequest("Test Product", 5000L, 1);
        CreateOrderRequest createOrderRequest = new CreateOrderRequest("USD", List.of(item));

        MvcResult createResult = mockMvc.perform(post("/api/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createOrderRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        OrderResponse createdOrder = objectMapper.readValue(
            createResult.getResponse().getContentAsString(),
            OrderResponse.class
        );

        orderId = createdOrder.getOrderId();
        idempotencyKey = UUID.randomUUID().toString();
    }

    @Test
    void initiateCheckout_ShouldReturnBadRequestWhenIdempotencyKeyMissing() throws Exception {
        // Arrange
        CheckoutRequest request = new CheckoutRequest(orderId, "STRIPE");

        // Act & Assert
        mockMvc.perform(post("/api/checkout")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void initiateCheckout_ShouldReturnNotFoundWhenOrderDoesNotExist() throws Exception {
        // Arrange
        UUID nonExistentOrderId = UUID.randomUUID();
        CheckoutRequest request = new CheckoutRequest(nonExistentOrderId, "STRIPE");

        // Act & Assert
        mockMvc.perform(post("/api/checkout")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Idempotency-Key", idempotencyKey)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void initiateCheckout_ShouldReturnConflictWhenIdempotencyKeyReused() throws Exception {
        // Arrange
        CheckoutRequest request = new CheckoutRequest(orderId, "STRIPE");

        // Primer checkout (exitoso)
        mockMvc.perform(post("/api/checkout")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Idempotency-Key", idempotencyKey)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        // Act & Assert - Segundo intento con la misma clave (debe fallar)
        mockMvc.perform(post("/api/checkout")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Idempotency-Key", idempotencyKey)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void initiateCheckout_ShouldReturnBadRequestWhenOrderIdInvalid() throws Exception {
        // Arrange
        CheckoutRequest request = new CheckoutRequest(null, "STRIPE");

        // Act & Assert
        mockMvc.perform(post("/api/checkout")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Idempotency-Key", idempotencyKey)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void initiateCheckout_ShouldReturnBadRequestWhenProviderInvalid() throws Exception {
        // Arrange
        CheckoutRequest request = new CheckoutRequest(orderId, "");

        // Act & Assert
        mockMvc.perform(post("/api/checkout")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Idempotency-Key", idempotencyKey)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}

