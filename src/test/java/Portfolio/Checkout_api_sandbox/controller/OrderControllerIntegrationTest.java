package Portfolio.Checkout_api_sandbox.controller;

import Portfolio.Checkout_api_sandbox.dto.request.CreateOrderRequest;
import Portfolio.Checkout_api_sandbox.dto.request.OrderItemRequest;
import Portfolio.Checkout_api_sandbox.dto.response.OrderResponse;
import Portfolio.Checkout_api_sandbox.TestcontainersConfiguration;
import com.fasterxml.jackson.databind.ObjectMapper;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests de integración para OrderController.
 * Verifica el flujo completo desde HTTP hasta base de datos.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@Transactional
class OrderControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void createOrder_ShouldReturnCreatedOrder() throws Exception {
        // Arrange
        OrderItemRequest item1 = new OrderItemRequest("T-shirt", 1999L, 2);
        OrderItemRequest item2 = new OrderItemRequest("Cap", 1299L, 1);
        CreateOrderRequest request = new CreateOrderRequest("USD", List.of(item1, item2));

        // Act & Assert
        MvcResult result = mockMvc.perform(post("/api/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.orderId").exists())
                .andExpect(jsonPath("$.status").value("CREATED"))
                .andExpect(jsonPath("$.currency").value("USD"))
                .andExpect(jsonPath("$.totalAmountMinor").value(5297))
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.items.length()").value(2))
                .andExpect(jsonPath("$.items[0].subtotalMinor").value(3998))
                .andExpect(jsonPath("$.items[1].subtotalMinor").value(1299))
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        OrderResponse orderResponse = objectMapper.readValue(responseBody, OrderResponse.class);

        assertNotNull(orderResponse.getOrderId());
        assertNotNull(orderResponse.getCreatedAt());
    }

    @Test
    void createOrder_ShouldReturnBadRequestWhenCurrencyInvalid() throws Exception {
        // Arrange
        OrderItemRequest item = new OrderItemRequest("Product", 1000L, 1);
        CreateOrderRequest request = new CreateOrderRequest("us", List.of(item)); // Inválida: minúscula

        // Act & Assert
        mockMvc.perform(post("/api/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Error de validación"))
                .andExpect(jsonPath("$.errors").exists());
    }

    @Test
    void createOrder_ShouldReturnBadRequestWhenItemsEmpty() throws Exception {
        // Arrange
        CreateOrderRequest request = new CreateOrderRequest("USD", List.of());

        // Act & Assert
        mockMvc.perform(post("/api/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createOrder_ShouldReturnBadRequestWhenPriceNegative() throws Exception {
        // Arrange
        OrderItemRequest item = new OrderItemRequest("Product", -1000L, 1);
        CreateOrderRequest request = new CreateOrderRequest("USD", List.of(item));

        // Act & Assert
        mockMvc.perform(post("/api/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getOrder_ShouldReturnOrderWhenExists() throws Exception {
        // Arrange - Crear orden primero
        OrderItemRequest item = new OrderItemRequest("Product", 5000L, 1);
        CreateOrderRequest createRequest = new CreateOrderRequest("USD", List.of(item));

        MvcResult createResult = mockMvc.perform(post("/api/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        OrderResponse createdOrder = objectMapper.readValue(
            createResult.getResponse().getContentAsString(),
            OrderResponse.class
        );

        // Act & Assert - Consultar la orden creada
        mockMvc.perform(get("/api/orders/" + createdOrder.getOrderId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(createdOrder.getOrderId().toString()))
                .andExpect(jsonPath("$.status").value("CREATED"))
                .andExpect(jsonPath("$.totalAmountMinor").value(5000));
    }

    @Test
    void getOrder_ShouldReturnNotFoundWhenOrderDoesNotExist() throws Exception {
        // Arrange
        UUID nonExistentId = UUID.randomUUID();

        // Act & Assert
        mockMvc.perform(get("/api/orders/" + nonExistentId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void createOrder_ShouldCalculateTotalsCorrectlyForMultipleItems() throws Exception {
        // Arrange
        OrderItemRequest item1 = new OrderItemRequest("Item A", 100L, 10);
        OrderItemRequest item2 = new OrderItemRequest("Item B", 250L, 4);
        OrderItemRequest item3 = new OrderItemRequest("Item C", 500L, 2);
        CreateOrderRequest request = new CreateOrderRequest("EUR", List.of(item1, item2, item3));

        // Act & Assert
        mockMvc.perform(post("/api/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.totalAmountMinor").value(3000)) // 1000 + 1000 + 1000
                .andExpect(jsonPath("$.items[0].subtotalMinor").value(1000))
                .andExpect(jsonPath("$.items[1].subtotalMinor").value(1000))
                .andExpect(jsonPath("$.items[2].subtotalMinor").value(1000));
    }
}

