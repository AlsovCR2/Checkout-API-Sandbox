package Portfolio.Checkout_api_sandbox.service;

import Portfolio.Checkout_api_sandbox.exception.OrderNotFoundException;
import Portfolio.Checkout_api_sandbox.exception.PaymentNotFoundException;
import Portfolio.Checkout_api_sandbox.integration.stripe.StripeWebhookValidator;
import Portfolio.Checkout_api_sandbox.model.OrderEntity;
import Portfolio.Checkout_api_sandbox.model.OrderStatus;
import Portfolio.Checkout_api_sandbox.model.PaymentEntity;
import Portfolio.Checkout_api_sandbox.model.PaymentStatus;
import Portfolio.Checkout_api_sandbox.repository.OrderRepository;
import Portfolio.Checkout_api_sandbox.repository.PaymentRepository;
import com.stripe.model.Event;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Service para procesar webhooks de Stripe.
 * Actualiza el estado de órdenes y pagos según los eventos recibidos.
 */
@Service
public class WebhookService {

    private static final Logger logger = LoggerFactory.getLogger(WebhookService.class);

    @Autowired
    private StripeWebhookValidator webhookValidator;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    /**
     * Procesa un webhook de Stripe.
     * Valida la firma y actualiza el estado según el tipo de evento.
     *
     * @param payload Cuerpo raw del webhook (JSON)
     * @param signatureHeader Valor del header "Stripe-Signature"
     * @throws Portfolio.Checkout_api_sandbox.exception.InvalidWebhookSignatureException si la firma es inválida
     */
    @Transactional
    public void processStripeWebhook(String payload, String signatureHeader) {
        logger.info("Processing Stripe webhook");

        // 1. Validar firma del webhook (CRÍTICO para seguridad)
        Event event = webhookValidator.validateAndConstructEvent(payload, signatureHeader);

        logger.info("Webhook validated - Event type: {}, Event ID: {}",
                    event.getType(), event.getId());

        // 2. Procesar según el tipo de evento
        if (webhookValidator.isPaymentSucceeded(event)) {
            handlePaymentSuccess(event);
        } else if (webhookValidator.isPaymentFailed(event)) {
            handlePaymentFailed(event);
        } else if (webhookValidator.isPaymentCanceled(event)) {
            handlePaymentCanceled(event);
        } else {
            logger.info("Webhook event type not handled: {}", event.getType());
        }
    }

    /**
     * Maneja el evento payment_intent.succeeded.
     * Marca la orden como PAID y el pago como SUCCEEDED.
     *
     * @param event Evento de Stripe
     */
    private void handlePaymentSuccess(Event event) {
        logger.info("Handling payment success event");

        try {
            // Extraer datos del evento
            String paymentIntentId = webhookValidator.extractPaymentIntentId(event);
            String orderIdStr = webhookValidator.extractOrderIdFromMetadata(event);
            UUID orderId = UUID.fromString(orderIdStr);

            logger.info("Payment succeeded - PaymentIntent: {}, Order: {}",
                       paymentIntentId, orderId);

            // Buscar la orden
            OrderEntity order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new OrderNotFoundException(orderId));

            // Buscar el pago
            PaymentEntity payment = paymentRepository.findByExternalPaymentId(paymentIntentId)
                    .orElseThrow(() -> new PaymentNotFoundException(
                        "Pago no encontrado con external ID: " + paymentIntentId));

            // Verificar que no esté ya procesado (idempotencia de webhooks)
            if (order.getStatus() == OrderStatus.PAID) {
                logger.warn("Order {} already marked as PAID, skipping", orderId);
                return;
            }

            // Actualizar estado del pago
            payment.setStatus(PaymentStatus.SUCCEEDED);
            paymentRepository.save(payment);

            // Actualizar estado de la orden
            order.setStatus(OrderStatus.PAID);
            orderRepository.save(order);

            logger.info("Order {} successfully marked as PAID", orderId);

        } catch (Exception e) {
            logger.error("Error handling payment success event: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Maneja el evento payment_intent.payment_failed.
     * Marca la orden como FAILED y el pago como FAILED.
     *
     * @param event Evento de Stripe
     */
    private void handlePaymentFailed(Event event) {
        logger.info("Handling payment failed event");

        try {
            // Extraer datos del evento
            String paymentIntentId = webhookValidator.extractPaymentIntentId(event);
            String orderIdStr = webhookValidator.extractOrderIdFromMetadata(event);
            UUID orderId = UUID.fromString(orderIdStr);

            logger.warn("Payment failed - PaymentIntent: {}, Order: {}",
                       paymentIntentId, orderId);

            // Buscar la orden
            OrderEntity order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new OrderNotFoundException(orderId));

            // Buscar el pago
            PaymentEntity payment = paymentRepository.findByExternalPaymentId(paymentIntentId)
                    .orElseThrow(() -> new PaymentNotFoundException(
                        "Pago no encontrado con external ID: " + paymentIntentId));

            // Verificar idempotencia
            if (order.getStatus() == OrderStatus.FAILED) {
                logger.warn("Order {} already marked as FAILED, skipping", orderId);
                return;
            }

            // Actualizar estado del pago
            payment.setStatus(PaymentStatus.FAILED);
            paymentRepository.save(payment);

            // Actualizar estado de la orden
            order.setStatus(OrderStatus.FAILED);
            orderRepository.save(order);

            logger.info("Order {} marked as FAILED", orderId);

        } catch (Exception e) {
            logger.error("Error handling payment failed event: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Maneja el evento payment_intent.canceled.
     * Marca la orden como CANCELED y el pago como CANCELED.
     *
     * @param event Evento de Stripe
     */
    private void handlePaymentCanceled(Event event) {
        logger.info("Handling payment canceled event");

        try {
            // Extraer datos del evento
            String paymentIntentId = webhookValidator.extractPaymentIntentId(event);
            String orderIdStr = webhookValidator.extractOrderIdFromMetadata(event);
            UUID orderId = UUID.fromString(orderIdStr);

            logger.info("Payment canceled - PaymentIntent: {}, Order: {}",
                       paymentIntentId, orderId);

            // Buscar la orden
            OrderEntity order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new OrderNotFoundException(orderId));

            // Buscar el pago
            PaymentEntity payment = paymentRepository.findByExternalPaymentId(paymentIntentId)
                    .orElseThrow(() -> new PaymentNotFoundException(
                        "Pago no encontrado con external ID: " + paymentIntentId));

            // Verificar idempotencia
            if (order.getStatus() == OrderStatus.CANCELED) {
                logger.warn("Order {} already marked as CANCELED, skipping", orderId);
                return;
            }

            // Actualizar estado del pago
            payment.setStatus(PaymentStatus.CANCELED);
            paymentRepository.save(payment);

            // Actualizar estado de la orden
            order.setStatus(OrderStatus.CANCELED);
            orderRepository.save(order);

            logger.info("Order {} marked as CANCELED", orderId);

        } catch (Exception e) {
            logger.error("Error handling payment canceled event: {}", e.getMessage(), e);
            throw e;
        }
    }
}

