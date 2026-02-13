package Portfolio.Checkout_api_sandbox.service;

import Portfolio.Checkout_api_sandbox.dto.request.CheckoutRequest;
import Portfolio.Checkout_api_sandbox.dto.response.CheckoutResponse;
import Portfolio.Checkout_api_sandbox.exception.IdempotencyConflictException;
import Portfolio.Checkout_api_sandbox.exception.InvalidOrderStateException;
import Portfolio.Checkout_api_sandbox.exception.OrderNotFoundException;
import Portfolio.Checkout_api_sandbox.integration.stripe.StripePaymentClient;
import Portfolio.Checkout_api_sandbox.mapper.PaymentMapper;
import Portfolio.Checkout_api_sandbox.model.*;
import Portfolio.Checkout_api_sandbox.repository.OrderRepository;
import Portfolio.Checkout_api_sandbox.repository.PaymentRepository;
import com.stripe.model.PaymentIntent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests unitarios para CheckoutService.
 * Verifica la lógica de idempotencia y validaciones de checkout.
 */
@ExtendWith(MockitoExtension.class)
class CheckoutServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private StripePaymentClient stripePaymentClient;

    @Mock
    private PaymentMapper paymentMapper;

    @InjectMocks
    private CheckoutService checkoutService;

    private UUID orderId;
    private String idempotencyKey;
    private CheckoutRequest checkoutRequest;
    private OrderEntity order;
    private PaymentIntent paymentIntent;
    private PaymentEntity paymentEntity;
    private CheckoutResponse checkoutResponse;

    @BeforeEach
    void setUp() {
        orderId = UUID.randomUUID();
        idempotencyKey = UUID.randomUUID().toString();

        checkoutRequest = new CheckoutRequest(orderId, "STRIPE");

        order = new OrderEntity();
        order.setId(orderId);
        order.setStatus(OrderStatus.CREATED);
        order.setCurrency("USD");
        order.setTotalAmountMinor(5297L);
        order.setItems(new ArrayList<>());

        OrderItemEntity item = new OrderItemEntity();
        item.setName("Test Item");
        item.setUnitPriceMinor(5297L);
        item.setQuantity(1);
        item.setSubtotalMinor(5297L);
        order.getItems().add(item);

        paymentIntent = new PaymentIntent();
        paymentIntent.setId("pi_test_123");
        paymentIntent.setClientSecret("pi_test_123_secret_456");
        paymentIntent.setStatus("requires_payment_method");

        paymentEntity = new PaymentEntity();
        paymentEntity.setId(UUID.randomUUID());
        paymentEntity.setOrder(order);
        paymentEntity.setExternalPaymentId("pi_test_123");
        paymentEntity.setClientSecret("pi_test_123_secret_456");
        paymentEntity.setStatus(PaymentStatus.INITIATED);

        checkoutResponse = new CheckoutResponse();
        checkoutResponse.setOrderId(orderId);
        checkoutResponse.setClientSecret("pi_test_123_secret_456");
        checkoutResponse.setStatus(OrderStatus.PAYMENT_PENDING);
    }

    @Test
    void initiateCheckout_ShouldSucceedWithValidOrder() {
        // Arrange
        when(paymentRepository.findByIdempotencyKey(idempotencyKey)).thenReturn(Optional.empty());
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(stripePaymentClient.createPaymentIntent(anyLong(), anyString(), any(UUID.class)))
            .thenReturn(paymentIntent);
        when(paymentRepository.save(any(PaymentEntity.class))).thenReturn(paymentEntity);
        when(orderRepository.save(any(OrderEntity.class))).thenReturn(order);
        when(paymentMapper.toCheckoutResponse(any(PaymentEntity.class))).thenReturn(checkoutResponse);

        // Act
        CheckoutResponse result = checkoutService.initiateCheckout(checkoutRequest, idempotencyKey);

        // Assert
        assertNotNull(result);
        assertEquals(orderId, result.getOrderId());
        assertNotNull(result.getClientSecret());

        verify(paymentRepository).findByIdempotencyKey(idempotencyKey);
        verify(orderRepository).findById(orderId);
        verify(stripePaymentClient).createPaymentIntent(5297L, "USD", orderId);
        verify(paymentRepository).save(any(PaymentEntity.class));
        verify(orderRepository).save(order);
    }

    @Test
    void initiateCheckout_ShouldThrowIdempotencyConflictWhenKeyExists() {
        // Arrange
        when(paymentRepository.findByIdempotencyKey(idempotencyKey))
            .thenReturn(Optional.of(paymentEntity));

        // Act & Assert
        IdempotencyConflictException exception = assertThrows(
            IdempotencyConflictException.class,
            () -> checkoutService.initiateCheckout(checkoutRequest, idempotencyKey)
        );

        assertTrue(exception.getMessage().contains(idempotencyKey));
        verify(paymentRepository).findByIdempotencyKey(idempotencyKey);
        verify(orderRepository, never()).findById(any());
        verify(stripePaymentClient, never()).createPaymentIntent(anyLong(), anyString(), any());
    }

    @Test
    void initiateCheckout_ShouldThrowOrderNotFoundWhenOrderDoesNotExist() {
        // Arrange
        when(paymentRepository.findByIdempotencyKey(idempotencyKey)).thenReturn(Optional.empty());
        when(orderRepository.findById(orderId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(
            OrderNotFoundException.class,
            () -> checkoutService.initiateCheckout(checkoutRequest, idempotencyKey)
        );

        verify(orderRepository).findById(orderId);
        verify(stripePaymentClient, never()).createPaymentIntent(anyLong(), anyString(), any());
    }

    @Test
    void initiateCheckout_ShouldThrowExceptionWhenOrderAlreadyPaid() {
        // Arrange
        order.setStatus(OrderStatus.PAID);
        when(paymentRepository.findByIdempotencyKey(idempotencyKey)).thenReturn(Optional.empty());
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        // Act & Assert
        InvalidOrderStateException exception = assertThrows(
            InvalidOrderStateException.class,
            () -> checkoutService.initiateCheckout(checkoutRequest, idempotencyKey)
        );

        assertTrue(exception.getMessage().contains("ya fue pagada"));
        verify(stripePaymentClient, never()).createPaymentIntent(anyLong(), anyString(), any());
    }

    @Test
    void initiateCheckout_ShouldThrowExceptionWhenOrderInPaymentPending() {
        // Arrange
        order.setStatus(OrderStatus.PAYMENT_PENDING);
        when(paymentRepository.findByIdempotencyKey(idempotencyKey)).thenReturn(Optional.empty());
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        // Act & Assert
        InvalidOrderStateException exception = assertThrows(
            InvalidOrderStateException.class,
            () -> checkoutService.initiateCheckout(checkoutRequest, idempotencyKey)
        );

        assertTrue(exception.getMessage().contains("pago en proceso"));
        verify(stripePaymentClient, never()).createPaymentIntent(anyLong(), anyString(), any());
    }

    @Test
    void initiateCheckout_ShouldThrowExceptionWhenOrderIsFailed() {
        // Arrange
        order.setStatus(OrderStatus.FAILED);
        when(paymentRepository.findByIdempotencyKey(idempotencyKey)).thenReturn(Optional.empty());
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        // Act & Assert
        InvalidOrderStateException exception = assertThrows(
            InvalidOrderStateException.class,
            () -> checkoutService.initiateCheckout(checkoutRequest, idempotencyKey)
        );

        assertTrue(exception.getMessage().contains("pago fallido"));
        verify(stripePaymentClient, never()).createPaymentIntent(anyLong(), anyString(), any());
    }

    @Test
    void initiateCheckout_ShouldThrowExceptionWhenOrderHasNoItems() {
        // Arrange
        order.getItems().clear();
        when(paymentRepository.findByIdempotencyKey(idempotencyKey)).thenReturn(Optional.empty());
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        // Act & Assert
        InvalidOrderStateException exception = assertThrows(
            InvalidOrderStateException.class,
            () -> checkoutService.initiateCheckout(checkoutRequest, idempotencyKey)
        );

        assertTrue(exception.getMessage().contains("no tiene items"));
        verify(stripePaymentClient, never()).createPaymentIntent(anyLong(), anyString(), any());
    }

    @Test
    void initiateCheckout_ShouldThrowExceptionWhenTotalIsZero() {
        // Arrange
        order.setTotalAmountMinor(0L);
        when(paymentRepository.findByIdempotencyKey(idempotencyKey)).thenReturn(Optional.empty());
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        // Act & Assert
        InvalidOrderStateException exception = assertThrows(
            InvalidOrderStateException.class,
            () -> checkoutService.initiateCheckout(checkoutRequest, idempotencyKey)
        );

        assertTrue(exception.getMessage().contains("mayor a cero"));
        verify(stripePaymentClient, never()).createPaymentIntent(anyLong(), anyString(), any());
    }

    @Test
    void isIdempotencyKeyUsed_ShouldReturnTrueWhenExists() {
        // Arrange
        when(paymentRepository.findByIdempotencyKey(idempotencyKey))
            .thenReturn(Optional.of(paymentEntity));

        // Act
        boolean result = checkoutService.isIdempotencyKeyUsed(idempotencyKey);

        // Assert
        assertTrue(result);
        verify(paymentRepository).findByIdempotencyKey(idempotencyKey);
    }

    @Test
    void isIdempotencyKeyUsed_ShouldReturnFalseWhenNotExists() {
        // Arrange
        when(paymentRepository.findByIdempotencyKey(idempotencyKey))
            .thenReturn(Optional.empty());

        // Act
        boolean result = checkoutService.isIdempotencyKeyUsed(idempotencyKey);

        // Assert
        assertFalse(result);
        verify(paymentRepository).findByIdempotencyKey(idempotencyKey);
    }
}

