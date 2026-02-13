package Portfolio.Checkout_api_sandbox.mapper;

import Portfolio.Checkout_api_sandbox.dto.request.CreateOrderRequest;
import Portfolio.Checkout_api_sandbox.dto.request.OrderItemRequest;
import Portfolio.Checkout_api_sandbox.dto.response.OrderResponse;
import Portfolio.Checkout_api_sandbox.model.OrderEntity;
import Portfolio.Checkout_api_sandbox.model.OrderStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitarios para OrderMapper.
 * Verifica los cálculos de subtotales y totales.
 */
class OrderMapperTest {

    private OrderMapper orderMapper;

    @BeforeEach
    void setUp() {
        orderMapper = new OrderMapper();
    }

    @Test
    void toEntity_ShouldCalculateSubtotalsCorrectly() {
        // Arrange
        OrderItemRequest item1 = new OrderItemRequest("T-shirt", 1999L, 2);
        OrderItemRequest item2 = new OrderItemRequest("Cap", 1299L, 1);
        CreateOrderRequest request = new CreateOrderRequest("USD", List.of(item1, item2));

        // Act
        OrderEntity result = orderMapper.toEntity(request);

        // Assert
        assertNotNull(result);
        assertEquals("USD", result.getCurrency());
        assertEquals(OrderStatus.CREATED, result.getStatus());
        assertEquals(2, result.getItems().size());

        // Verificar subtotal del primer item (1999 * 2 = 3998)
        assertEquals(3998L, result.getItems().get(0).getSubtotalMinor());

        // Verificar subtotal del segundo item (1299 * 1 = 1299)
        assertEquals(1299L, result.getItems().get(1).getSubtotalMinor());

        // Verificar total (3998 + 1299 = 5297)
        assertEquals(5297L, result.getTotalAmountMinor());
    }

    @Test
    void toEntity_ShouldHandleSingleItem() {
        // Arrange
        OrderItemRequest item = new OrderItemRequest("Product", 5000L, 3);
        CreateOrderRequest request = new CreateOrderRequest("EUR", List.of(item));

        // Act
        OrderEntity result = orderMapper.toEntity(request);

        // Assert
        assertEquals(1, result.getItems().size());
        assertEquals(15000L, result.getItems().get(0).getSubtotalMinor()); // 5000 * 3
        assertEquals(15000L, result.getTotalAmountMinor());
    }

    @Test
    void toEntity_ShouldSetBidirectionalRelationship() {
        // Arrange
        OrderItemRequest item = new OrderItemRequest("Product", 1000L, 1);
        CreateOrderRequest request = new CreateOrderRequest("USD", List.of(item));

        // Act
        OrderEntity result = orderMapper.toEntity(request);

        // Assert
        assertNotNull(result.getItems().get(0).getOrder());
        assertEquals(result, result.getItems().get(0).getOrder());
    }

    @Test
    void toResponse_ShouldMapAllFields() {
        // Arrange
        OrderEntity entity = new OrderEntity();
        entity.setCurrency("USD");
        entity.setStatus(OrderStatus.PAID);
        entity.setTotalAmountMinor(5297L);

        // Act
        OrderResponse result = orderMapper.toResponse(entity);

        // Assert
        assertNotNull(result);
        assertEquals("USD", result.getCurrency());
        assertEquals(OrderStatus.PAID, result.getStatus());
        assertEquals(5297L, result.getTotalAmountMinor());
    }

    @Test
    void toEntity_ShouldCalculateCorrectlyWithMultipleQuantities() {
        // Arrange
        OrderItemRequest item1 = new OrderItemRequest("Item A", 100L, 5);
        OrderItemRequest item2 = new OrderItemRequest("Item B", 200L, 3);
        OrderItemRequest item3 = new OrderItemRequest("Item C", 150L, 2);
        CreateOrderRequest request = new CreateOrderRequest("USD", List.of(item1, item2, item3));

        // Act
        OrderEntity result = orderMapper.toEntity(request);

        // Assert
        assertEquals(500L, result.getItems().get(0).getSubtotalMinor()); // 100 * 5
        assertEquals(600L, result.getItems().get(1).getSubtotalMinor()); // 200 * 3
        assertEquals(300L, result.getItems().get(2).getSubtotalMinor()); // 150 * 2
        assertEquals(1400L, result.getTotalAmountMinor()); // 500 + 600 + 300
    }
}

