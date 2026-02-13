package Portfolio.Checkout_api_sandbox.service;

import Portfolio.Checkout_api_sandbox.dto.request.CheckoutRequest;
import Portfolio.Checkout_api_sandbox.dto.response.CheckoutResponse;
import Portfolio.Checkout_api_sandbox.exception.IdempotencyConflictException;
import Portfolio.Checkout_api_sandbox.exception.InvalidOrderStateException;
import Portfolio.Checkout_api_sandbox.exception.OrderNotFoundException;
import Portfolio.Checkout_api_sandbox.integration.stripe.StripePaymentClient;
import Portfolio.Checkout_api_sandbox.mapper.PaymentMapper;
import Portfolio.Checkout_api_sandbox.model.OrderEntity;
import Portfolio.Checkout_api_sandbox.model.OrderStatus;
import Portfolio.Checkout_api_sandbox.model.PaymentEntity;
import Portfolio.Checkout_api_sandbox.model.PaymentProvider;
import Portfolio.Checkout_api_sandbox.model.PaymentStatus;
import Portfolio.Checkout_api_sandbox.repository.OrderRepository;
import Portfolio.Checkout_api_sandbox.repository.PaymentRepository;
import com.stripe.model.PaymentIntent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Service para gestionar el proceso de checkout.
 * Maneja la creación de Payment Intents en Stripe con idempotencia.
 */
@Service
public class CheckoutService {

    private static final Logger logger = LoggerFactory.getLogger(CheckoutService.class);

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private StripePaymentClient stripePaymentClient;

    @Autowired
    private PaymentMapper paymentMapper;

    /**
     * Inicia el proceso de checkout para una orden.
     * Implementa idempotencia mediante la clave de idempotencia.
     *
     * @param request Datos del checkout (orderId y provider)
     * @param idempotencyKey Clave única para prevenir pagos duplicados
     * @return CheckoutResponse con el client_secret para el frontend
     * @throws OrderNotFoundException si la orden no existe
     * @throws InvalidOrderStateException si la orden no está en estado válido
     * @throws IdempotencyConflictException si la clave ya fue usada
     */
    @Transactional
    public CheckoutResponse initiateCheckout(CheckoutRequest request, String idempotencyKey) {
        logger.info("Initiating checkout for order {} with idempotency key: {}",
                    request.getOrderId(), idempotencyKey);

        // 1. Verificar idempotencia
        Optional<PaymentEntity> existingPayment = paymentRepository.findByIdempotencyKey(idempotencyKey);
        if (existingPayment.isPresent()) {
            logger.warn("Idempotency conflict detected for key: {}", idempotencyKey);
            throw new IdempotencyConflictException(idempotencyKey);
        }

        // 2. Buscar la orden
        OrderEntity order = orderRepository.findById(request.getOrderId())
                .orElseThrow(() -> new OrderNotFoundException(request.getOrderId()));

        // 3. Validar estado de la orden
        validateOrderForCheckout(order);

        // 4. Crear Payment Intent en Stripe
        PaymentIntent paymentIntent = stripePaymentClient.createPaymentIntent(
                order.getTotalAmountMinor(),
                order.getCurrency(),
                order.getId()
        );

        logger.info("Payment Intent created in Stripe - ID: {}, Status: {}",
                    paymentIntent.getId(), paymentIntent.getStatus());

        // 5. Crear registro de pago en la base de datos
        PaymentEntity payment = new PaymentEntity();
        payment.setOrder(order);
        payment.setProvider(PaymentProvider.valueOf(request.getProvider().toUpperCase()));
        payment.setExternalPaymentId(paymentIntent.getId());
        payment.setClientSecret(paymentIntent.getClientSecret());
        payment.setStatus(PaymentStatus.INITIATED);
        payment.setAmountMinor(order.getTotalAmountMinor());
        payment.setCurrency(order.getCurrency());
        payment.setIdempotencyKey(idempotencyKey);

        PaymentEntity savedPayment = paymentRepository.save(payment);

        logger.info("Payment record created - ID: {}", savedPayment.getId());

        // 6. Actualizar estado de la orden a PAYMENT_PENDING
        order.setStatus(OrderStatus.PAYMENT_PENDING);
        orderRepository.save(order);

        logger.info("Order {} status updated to PAYMENT_PENDING", order.getId());

        // 7. Retornar respuesta con client_secret
        return paymentMapper.toCheckoutResponse(savedPayment);
    }

    /**
     * Valida que la orden esté en un estado apropiado para iniciar el checkout.
     *
     * @param order Orden a validar
     * @throws InvalidOrderStateException si la orden no está en estado válido
     */
    private void validateOrderForCheckout(OrderEntity order) {
        OrderStatus status = order.getStatus();

        // Solo se puede hacer checkout de órdenes en estado CREATED
        if (status != OrderStatus.CREATED) {
            String message = buildInvalidStateMessage(status);
            logger.warn("Invalid order state for checkout - Order: {}, Status: {}",
                       order.getId(), status);
            throw new InvalidOrderStateException(message);
        }

        // Validar que el total sea mayor a cero
        if (order.getTotalAmountMinor() == null || order.getTotalAmountMinor() <= 0) {
            logger.warn("Order {} has invalid total amount: {}",
                       order.getId(), order.getTotalAmountMinor());
            throw new InvalidOrderStateException("El monto total de la orden debe ser mayor a cero");
        }

        // Validar que tenga items
        if (order.getItems() == null || order.getItems().isEmpty()) {
            logger.warn("Order {} has no items", order.getId());
            throw new InvalidOrderStateException("La orden no tiene items");
        }
    }

    /**
     * Construye un mensaje descriptivo para estados inválidos.
     */
    private String buildInvalidStateMessage(OrderStatus status) {
        return switch (status) {
            case PAYMENT_PENDING -> "La orden ya tiene un pago en proceso";
            case PAID -> "La orden ya fue pagada";
            case FAILED -> "La orden tiene un pago fallido, cree una nueva orden";
            case CANCELED -> "La orden está cancelada, cree una nueva orden";
            default -> "La orden no está en un estado válido para checkout";
        };
    }

    /**
     * Verifica si una clave de idempotencia ya fue usada.
     *
     * @param idempotencyKey Clave a verificar
     * @return true si ya existe
     */
    @Transactional(readOnly = true)
    public boolean isIdempotencyKeyUsed(String idempotencyKey) {
        return paymentRepository.findByIdempotencyKey(idempotencyKey).isPresent();
    }
}

