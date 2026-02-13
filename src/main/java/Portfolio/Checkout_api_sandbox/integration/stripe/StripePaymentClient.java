package Portfolio.Checkout_api_sandbox.integration.stripe;

import Portfolio.Checkout_api_sandbox.exception.StripeApiException;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Cliente para interactuar con la API de Stripe.
 * Maneja la creación de Payment Intents y gestión de pagos.
 */
@Component
public class StripePaymentClient {

    private static final Logger logger = LoggerFactory.getLogger(StripePaymentClient.class);

    /**
     * Crea un Payment Intent en Stripe.
     *
     * @param amountMinor Monto en unidades menores (centavos)
     * @param currency Código de moneda ISO 4217 (USD, EUR, MXN, etc.)
     * @param orderId ID de la orden asociada (se guarda en metadata)
     * @return PaymentIntent creado con el client_secret
     * @throws StripeApiException si hay error al comunicarse con Stripe
     */
    public PaymentIntent createPaymentIntent(Long amountMinor, String currency, UUID orderId) {
        try {
            logger.info("Creating Payment Intent for order {} - Amount: {} {}",
                        orderId, amountMinor, currency.toUpperCase());

            // Preparar metadata para asociar el pago con la orden
            Map<String, String> metadata = new HashMap<>();
            metadata.put("orderId", orderId.toString());

            // Construir parámetros del Payment Intent
            PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                    .setAmount(amountMinor)
                    .setCurrency(currency.toLowerCase())
                    .putAllMetadata(metadata)
                    // Métodos de pago automáticos basados en la moneda
                    .setAutomaticPaymentMethods(
                            PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                                    .setEnabled(true)
                                    .build()
                    )
                    .build();

            // Crear el Payment Intent en Stripe
            PaymentIntent paymentIntent = PaymentIntent.create(params);

            logger.info("Payment Intent created successfully - ID: {}, Status: {}",
                        paymentIntent.getId(), paymentIntent.getStatus());

            return paymentIntent;

        } catch (StripeException e) {
            logger.error("Stripe API error while creating Payment Intent for order {}: {}",
                         orderId, e.getMessage(), e);
            throw new StripeApiException("Error al crear Payment Intent: " + e.getUserMessage(), e);
        } catch (Exception e) {
            logger.error("Unexpected error while creating Payment Intent for order {}: {}",
                         orderId, e.getMessage(), e);
            throw new StripeApiException("Error inesperado al crear Payment Intent", e);
        }
    }

    /**
     * Recupera un Payment Intent existente desde Stripe.
     *
     * @param paymentIntentId ID del Payment Intent en Stripe (pi_xxx)
     * @return PaymentIntent recuperado
     * @throws StripeApiException si hay error al comunicarse con Stripe
     */
    public PaymentIntent retrievePaymentIntent(String paymentIntentId) {
        try {
            logger.debug("Retrieving Payment Intent: {}", paymentIntentId);

            PaymentIntent paymentIntent = PaymentIntent.retrieve(paymentIntentId);

            logger.debug("Payment Intent retrieved - Status: {}", paymentIntent.getStatus());

            return paymentIntent;

        } catch (StripeException e) {
            logger.error("Stripe API error while retrieving Payment Intent {}: {}",
                         paymentIntentId, e.getMessage(), e);
            throw new StripeApiException("Error al recuperar Payment Intent: " + e.getUserMessage(), e);
        } catch (Exception e) {
            logger.error("Unexpected error while retrieving Payment Intent {}: {}",
                         paymentIntentId, e.getMessage(), e);
            throw new StripeApiException("Error inesperado al recuperar Payment Intent", e);
        }
    }

    /**
     * Cancela un Payment Intent en Stripe.
     *
     * @param paymentIntentId ID del Payment Intent a cancelar
     * @return PaymentIntent cancelado
     * @throws StripeApiException si hay error al comunicarse con Stripe
     */
    public PaymentIntent cancelPaymentIntent(String paymentIntentId) {
        try {
            logger.info("Canceling Payment Intent: {}", paymentIntentId);

            PaymentIntent paymentIntent = PaymentIntent.retrieve(paymentIntentId);
            PaymentIntent canceledIntent = paymentIntent.cancel();

            logger.info("Payment Intent canceled successfully: {}", paymentIntentId);

            return canceledIntent;

        } catch (StripeException e) {
            logger.error("Stripe API error while canceling Payment Intent {}: {}",
                         paymentIntentId, e.getMessage(), e);
            throw new StripeApiException("Error al cancelar Payment Intent: " + e.getUserMessage(), e);
        } catch (Exception e) {
            logger.error("Unexpected error while canceling Payment Intent {}: {}",
                         paymentIntentId, e.getMessage(), e);
            throw new StripeApiException("Error inesperado al cancelar Payment Intent", e);
        }
    }
}

