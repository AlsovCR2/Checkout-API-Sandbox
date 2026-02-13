package Portfolio.Checkout_api_sandbox.service;

import Portfolio.Checkout_api_sandbox.dto.request.CreateOrderRequest;
import Portfolio.Checkout_api_sandbox.dto.request.OrderItemRequest;
import Portfolio.Checkout_api_sandbox.dto.response.OrderResponse;
import Portfolio.Checkout_api_sandbox.exception.OrderNotFoundException;
import Portfolio.Checkout_api_sandbox.mapper.OrderMapper;
import Portfolio.Checkout_api_sandbox.model.OrderEntity;
import Portfolio.Checkout_api_sandbox.model.OrderStatus;
import Portfolio.Checkout_api_sandbox.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests unitarios para OrderService.
 * Verifica la lógica de negocio de creación y consulta de órdenes.
 */
@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderMapper orderMapper;

    @InjectMocks
    private OrderService orderService;

    private CreateOrderRequest createOrderRequest;
    private OrderEntity orderEntity;
    private OrderResponse orderResponse;
    private UUID orderId;

    @BeforeEach
    void setUp() {
        orderId = UUID.randomUUID();

        // Request con 2 items
        OrderItemRequest item1 = new OrderItemRequest("T-shirt", 1999L, 2);
        OrderItemRequest item2 = new OrderItemRequest("Cap", 1299L, 1);
        createOrderRequest = new CreateOrderRequest("USD", List.of(item1, item2));

        // Entity simulada
        orderEntity = new OrderEntity();
        orderEntity.setId(orderId);
        orderEntity.setCurrency("USD");
        orderEntity.setTotalAmountMinor(5297L); // (1999*2) + (1299*1)
        orderEntity.setStatus(OrderStatus.CREATED);

        // Response simulada
        orderResponse = new OrderResponse();
        orderResponse.setOrderId(orderId);
        orderResponse.setStatus(OrderStatus.CREATED);
        orderResponse.setCurrency("USD");
        orderResponse.setTotalAmountMinor(5297L);
    }

    @Test
    void createOrder_ShouldCreateOrderSuccessfully() {
        // Arrange
        when(orderMapper.toEntity(createOrderRequest)).thenReturn(orderEntity);
        when(orderRepository.save(orderEntity)).thenReturn(orderEntity);
        when(orderMapper.toResponse(orderEntity)).thenReturn(orderResponse);

        // Act
        OrderResponse result = orderService.createOrder(createOrderRequest);

        // Assert
        assertNotNull(result);
        assertEquals(orderId, result.getOrderId());
        assertEquals("USD", result.getCurrency());
        assertEquals(5297L, result.getTotalAmountMinor());
        assertEquals(OrderStatus.CREATED, result.getStatus());

        verify(orderMapper).toEntity(createOrderRequest);
        verify(orderRepository).save(orderEntity);
        verify(orderMapper).toResponse(orderEntity);
    }

    @Test
    void createOrder_ShouldCalculateTotalsCorrectly() {
        // Arrange
        when(orderMapper.toEntity(any())).thenReturn(orderEntity);
        when(orderRepository.save(any())).thenReturn(orderEntity);
        when(orderMapper.toResponse(any())).thenReturn(orderResponse);

        // Act
        OrderResponse result = orderService.createOrder(createOrderRequest);

        // Assert
        assertEquals(5297L, result.getTotalAmountMinor());
        verify(orderRepository).save(orderEntity);
    }

    @Test
    void getOrder_ShouldReturnOrderWhenExists() {
        // Arrange
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(orderEntity));
        when(orderMapper.toResponse(orderEntity)).thenReturn(orderResponse);

        // Act
        OrderResponse result = orderService.getOrder(orderId);

        // Assert
        assertNotNull(result);
        assertEquals(orderId, result.getOrderId());
        assertEquals(OrderStatus.CREATED, result.getStatus());

        verify(orderRepository).findById(orderId);
        verify(orderMapper).toResponse(orderEntity);
    }

    @Test
    void getOrder_ShouldThrowExceptionWhenNotFound() {
        // Arrange
        when(orderRepository.findById(orderId)).thenReturn(Optional.empty());

        // Act & Assert
        OrderNotFoundException exception = assertThrows(
            OrderNotFoundException.class,
            () -> orderService.getOrder(orderId)
        );

        assertTrue(exception.getMessage().contains(orderId.toString()));
        verify(orderRepository).findById(orderId);
        verify(orderMapper, never()).toResponse(any());
    }

    @Test
    void updateOrderStatus_ShouldUpdateSuccessfully() {
        // Arrange
        OrderStatus newStatus = OrderStatus.PAYMENT_PENDING;
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(orderEntity));
        when(orderRepository.save(orderEntity)).thenReturn(orderEntity);

        // Act
        orderService.updateOrderStatus(orderId, newStatus);

        // Assert
        verify(orderRepository).findById(orderId);
        verify(orderRepository).save(orderEntity);
        assertEquals(newStatus, orderEntity.getStatus());
    }

    @Test
    void updateOrderStatus_ShouldThrowExceptionWhenOrderNotFound() {
        // Arrange
        when(orderRepository.findById(orderId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(
            OrderNotFoundException.class,
            () -> orderService.updateOrderStatus(orderId, OrderStatus.PAID)
        );

        verify(orderRepository).findById(orderId);
        verify(orderRepository, never()).save(any());
    }

    @Test
    void findOrderById_ShouldReturnEntityWhenExists() {
        // Arrange
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(orderEntity));

        // Act
        OrderEntity result = orderService.findOrderById(orderId);

        // Assert
        assertNotNull(result);
        assertEquals(orderId, result.getId());
        verify(orderRepository).findById(orderId);
    }

    @Test
    void findOrderById_ShouldThrowExceptionWhenNotFound() {
        // Arrange
        when(orderRepository.findById(orderId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(
            OrderNotFoundException.class,
            () -> orderService.findOrderById(orderId)
        );
    }
}

