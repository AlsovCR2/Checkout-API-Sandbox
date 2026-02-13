package Portfolio.Checkout_api_sandbox.service;

import Portfolio.Checkout_api_sandbox.exception.OrderNotFoundException;
import Portfolio.Checkout_api_sandbox.exception.PaymentNotFoundException;
import Portfolio.Checkout_api_sandbox.integration.stripe.StripeWebhookValidator;
import Portfolio.Checkout_api_sandbox.model.*;
import Portfolio.Checkout_api_sandbox.repository.OrderRepository;
import Portfolio.Checkout_api_sandbox.repository.PaymentRepository;
import com.stripe.model.Event;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Tests unitarios para WebhookService.
 * Verifica el procesamiento de eventos de Stripe.
 */
@ExtendWith(MockitoExtension.class)
class WebhookServiceTest {

    @Mock
    private StripeWebhookValidator webhookValidator;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @InjectMocks
    private WebhookService webhookService;

    @Mock
    private Event event;

    private UUID orderId;
    private String paymentIntentId;
    private String payload;
    private String signature;
    private OrderEntity order;
    private PaymentEntity payment;

    @BeforeEach
    void setUp() {
        orderId = UUID.randomUUID();
        paymentIntentId = "pi_test_123";
        payload = "{\"type\":\"payment_intent.succeeded\"}";
        signature = "t=123,v1=abc";

        order = new OrderEntity();
        order.setId(orderId);
        order.setStatus(OrderStatus.PAYMENT_PENDING);
        order.setCurrency("USD");
        order.setTotalAmountMinor(5297L);

        payment = new PaymentEntity();
        payment.setId(UUID.randomUUID());
        payment.setOrder(order);
        payment.setExternalPaymentId(paymentIntentId);
        payment.setStatus(PaymentStatus.INITIATED);
    }

    @Test
    void processStripeWebhook_ShouldHandlePaymentSucceeded() {
        // Arrange
        when(webhookValidator.validateAndConstructEvent(payload, signature)).thenReturn(event);
        when(webhookValidator.isPaymentSucceeded(event)).thenReturn(true);
        when(webhookValidator.extractPaymentIntentId(event)).thenReturn(paymentIntentId);
        when(webhookValidator.extractOrderIdFromMetadata(event)).thenReturn(orderId.toString());
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(paymentRepository.findByExternalPaymentId(paymentIntentId)).thenReturn(Optional.of(payment));

        // Act
        webhookService.processStripeWebhook(payload, signature);

        // Assert
        assertEquals(OrderStatus.PAID, order.getStatus());
        assertEquals(PaymentStatus.SUCCEEDED, payment.getStatus());
        verify(webhookValidator).validateAndConstructEvent(payload, signature);
        verify(orderRepository).save(order);
        verify(paymentRepository).save(payment);
    }

    @Test
    void processStripeWebhook_ShouldHandlePaymentFailed() {
        // Arrange
        when(webhookValidator.validateAndConstructEvent(payload, signature)).thenReturn(event);
        when(webhookValidator.isPaymentSucceeded(event)).thenReturn(false);
        when(webhookValidator.isPaymentFailed(event)).thenReturn(true);
        when(webhookValidator.extractPaymentIntentId(event)).thenReturn(paymentIntentId);
        when(webhookValidator.extractOrderIdFromMetadata(event)).thenReturn(orderId.toString());
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(paymentRepository.findByExternalPaymentId(paymentIntentId)).thenReturn(Optional.of(payment));

        // Act
        webhookService.processStripeWebhook(payload, signature);

        // Assert
        assertEquals(OrderStatus.FAILED, order.getStatus());
        assertEquals(PaymentStatus.FAILED, payment.getStatus());
        verify(webhookValidator).validateAndConstructEvent(payload, signature);
        verify(orderRepository).save(order);
        verify(paymentRepository).save(payment);
    }

    @Test
    void processStripeWebhook_ShouldHandlePaymentCanceled() {
        // Arrange
        when(webhookValidator.validateAndConstructEvent(payload, signature)).thenReturn(event);
        when(webhookValidator.isPaymentSucceeded(event)).thenReturn(false);
        when(webhookValidator.isPaymentFailed(event)).thenReturn(false);
        when(webhookValidator.isPaymentCanceled(event)).thenReturn(true);
        when(webhookValidator.extractPaymentIntentId(event)).thenReturn(paymentIntentId);
        when(webhookValidator.extractOrderIdFromMetadata(event)).thenReturn(orderId.toString());
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(paymentRepository.findByExternalPaymentId(paymentIntentId)).thenReturn(Optional.of(payment));

        // Act
        webhookService.processStripeWebhook(payload, signature);

        // Assert
        assertEquals(OrderStatus.CANCELED, order.getStatus());
        assertEquals(PaymentStatus.CANCELED, payment.getStatus());
        verify(webhookValidator).validateAndConstructEvent(payload, signature);
        verify(orderRepository).save(order);
        verify(paymentRepository).save(payment);
    }

    @Test
    void processStripeWebhook_ShouldSkipWhenOrderAlreadyPaid() {
        // Arrange
        order.setStatus(OrderStatus.PAID);
        when(webhookValidator.validateAndConstructEvent(payload, signature)).thenReturn(event);
        when(webhookValidator.isPaymentSucceeded(event)).thenReturn(true);
        when(webhookValidator.extractPaymentIntentId(event)).thenReturn(paymentIntentId);
        when(webhookValidator.extractOrderIdFromMetadata(event)).thenReturn(orderId.toString());
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(paymentRepository.findByExternalPaymentId(paymentIntentId)).thenReturn(Optional.of(payment));

        // Act
        webhookService.processStripeWebhook(payload, signature);

        // Assert
        assertEquals(OrderStatus.PAID, order.getStatus()); // No cambió
        verify(orderRepository, never()).save(order);
        verify(paymentRepository, never()).save(payment);
    }

    @Test
    void processStripeWebhook_ShouldThrowExceptionWhenOrderNotFound() {
        // Arrange
        when(webhookValidator.validateAndConstructEvent(payload, signature)).thenReturn(event);
        when(webhookValidator.isPaymentSucceeded(event)).thenReturn(true);
        when(webhookValidator.extractPaymentIntentId(event)).thenReturn(paymentIntentId);
        when(webhookValidator.extractOrderIdFromMetadata(event)).thenReturn(orderId.toString());
        when(orderRepository.findById(orderId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(
            OrderNotFoundException.class,
            () -> webhookService.processStripeWebhook(payload, signature)
        );

        verify(orderRepository).findById(orderId);
        verify(orderRepository, never()).save(any());
    }

    @Test
    void processStripeWebhook_ShouldThrowExceptionWhenPaymentNotFound() {
        // Arrange
        when(webhookValidator.validateAndConstructEvent(payload, signature)).thenReturn(event);
        when(webhookValidator.isPaymentSucceeded(event)).thenReturn(true);
        when(webhookValidator.extractPaymentIntentId(event)).thenReturn(paymentIntentId);
        when(webhookValidator.extractOrderIdFromMetadata(event)).thenReturn(orderId.toString());
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(paymentRepository.findByExternalPaymentId(paymentIntentId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(
            PaymentNotFoundException.class,
            () -> webhookService.processStripeWebhook(payload, signature)
        );

        verify(paymentRepository).findByExternalPaymentId(paymentIntentId);
        verify(orderRepository, never()).save(any());
    }

    @Test
    void processStripeWebhook_ShouldIgnoreUnhandledEventTypes() {
        // Arrange
        when(webhookValidator.validateAndConstructEvent(payload, signature)).thenReturn(event);
        when(webhookValidator.isPaymentSucceeded(event)).thenReturn(false);
        when(webhookValidator.isPaymentFailed(event)).thenReturn(false);
        when(webhookValidator.isPaymentCanceled(event)).thenReturn(false);

        // Act
        webhookService.processStripeWebhook(payload, signature);

        // Assert
        verify(webhookValidator).validateAndConstructEvent(payload, signature);
        verify(orderRepository, never()).findById(any());
        verify(paymentRepository, never()).findByExternalPaymentId(anyString());
    }
}

