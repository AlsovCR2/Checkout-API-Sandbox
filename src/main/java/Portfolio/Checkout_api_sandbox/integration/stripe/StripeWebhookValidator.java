package Portfolio.Checkout_api_sandbox.integration.stripe;

import Portfolio.Checkout_api_sandbox.exception.InvalidWebhookSignatureException;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.net.Webhook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Validador de webhooks de Stripe.
 * Verifica la autenticidad de los webhooks usando la firma HMAC.
 */
@Component
public class StripeWebhookValidator {

    private static final Logger logger = LoggerFactory.getLogger(StripeWebhookValidator.class);

    @Value("${stripe.webhookSecret}")
    private String webhookSecret;

    /**
     * Valida y construye un evento de Stripe desde el payload y la firma.
     *
     * IMPORTANTE: Esta validación es CRÍTICA para la seguridad.
     * Sin ella, cualquiera podría enviar webhooks falsos para marcar órdenes como pagadas.
     *
     * @param payload Cuerpo del webhook (JSON raw)
     * @param signatureHeader Valor del header "Stripe-Signature"
     * @return Event de Stripe validado
     * @throws InvalidWebhookSignatureException si la firma es inválida
     */
    public Event validateAndConstructEvent(String payload, String signatureHeader) {
        try {
            logger.debug("Validating webhook signature");

            // Verificar que tenemos el secret configurado
            if (webhookSecret == null || webhookSecret.isBlank()) {
                logger.error("Webhook secret is not configured!");
                throw new InvalidWebhookSignatureException("Webhook secret no configurado en el servidor");
            }

            // Stripe verifica la firma HMAC y construye el evento
            Event event = Webhook.constructEvent(payload, signatureHeader, webhookSecret);

            logger.info("Webhook signature validated successfully - Event type: {}, Event ID: {}",
                        event.getType(), event.getId());

            return event;

        } catch (SignatureVerificationException e) {
            logger.error("Invalid webhook signature - Possible attack attempt! Error: {}", e.getMessage());
            throw new InvalidWebhookSignatureException("Firma de webhook inválida - Verificación falló", e);
        } catch (Exception e) {
            logger.error("Unexpected error while validating webhook: {}", e.getMessage(), e);
            throw new InvalidWebhookSignatureException("Error al validar webhook", e);
        }
    }

    /**
     * Extrae el Payment Intent ID desde un evento de Stripe.
     *
     * @param event Evento de Stripe ya validado
     * @return ID del Payment Intent (pi_xxx)
     */
    public String extractPaymentIntentId(Event event) {
        try {
            // El objeto data.object contiene el PaymentIntent
            com.stripe.model.StripeObject stripeObject = event.getDataObjectDeserializer()
                    .getObject()
                    .orElseThrow(() -> new IllegalStateException("No data object in event"));

            if (stripeObject instanceof com.stripe.model.PaymentIntent) {
                com.stripe.model.PaymentIntent paymentIntent = (com.stripe.model.PaymentIntent) stripeObject;
                return paymentIntent.getId();
            }

            throw new IllegalArgumentException("Event does not contain a PaymentIntent object");

        } catch (Exception e) {
            logger.error("Error extracting PaymentIntent ID from event: {}", e.getMessage(), e);
            throw new IllegalArgumentException("No se pudo extraer el Payment Intent del evento", e);
        }
    }

    /**
     * Extrae el Order ID desde los metadata de un Payment Intent en el evento.
     *
     * @param event Evento de Stripe ya validado
     * @return UUID del Order
     */
    public String extractOrderIdFromMetadata(Event event) {
        try {
            com.stripe.model.StripeObject stripeObject = event.getDataObjectDeserializer()
                    .getObject()
                    .orElseThrow(() -> new IllegalStateException("No data object in event"));

            if (stripeObject instanceof com.stripe.model.PaymentIntent) {
                com.stripe.model.PaymentIntent paymentIntent = (com.stripe.model.PaymentIntent) stripeObject;

                if (paymentIntent.getMetadata() != null && paymentIntent.getMetadata().containsKey("orderId")) {
                    return paymentIntent.getMetadata().get("orderId");
                }
            }

            throw new IllegalArgumentException("OrderId not found in PaymentIntent metadata");

        } catch (Exception e) {
            logger.error("Error extracting orderId from event metadata: {}", e.getMessage(), e);
            throw new IllegalArgumentException("No se pudo extraer el orderId de los metadata", e);
        }
    }

    /**
     * Verifica si el evento es de tipo "payment_intent.succeeded".
     */
    public boolean isPaymentSucceeded(Event event) {
        return "payment_intent.succeeded".equals(event.getType());
    }

    /**
     * Verifica si el evento es de tipo "payment_intent.payment_failed".
     */
    public boolean isPaymentFailed(Event event) {
        return "payment_intent.payment_failed".equals(event.getType());
    }

    /**
     * Verifica si el evento es de tipo "payment_intent.canceled".
     */
    public boolean isPaymentCanceled(Event event) {
        return "payment_intent.canceled".equals(event.getType());
    }
}

