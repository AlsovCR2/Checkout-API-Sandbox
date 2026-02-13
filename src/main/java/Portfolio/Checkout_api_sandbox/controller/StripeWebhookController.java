package Portfolio.Checkout_api_sandbox.controller;

import Portfolio.Checkout_api_sandbox.service.WebhookService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controller REST para recibir webhooks de Stripe.
 *
 * SEGURIDAD CRÍTICA: Valida la firma del webhook antes de procesar.
 * Sin esta validación, atacantes podrían enviar webhooks falsos.
 */
@RestController
@RequestMapping("/api/webhooks")
@Tag(name = "Webhooks", description = "Endpoint para recibir eventos de Stripe")
public class StripeWebhookController {

    private static final Logger logger = LoggerFactory.getLogger(StripeWebhookController.class);

    @Autowired
    private WebhookService webhookService;

    /**
     * Recibe y procesa webhooks de Stripe.
     *
     * IMPORTANTE:
     * - Stripe envía el payload como raw body (String, no JSON deserializado)
     * - La firma viene en el header "Stripe-Signature"
     * - La validación de firma es CRÍTICA para seguridad
     *
     * Eventos soportados:
     * - payment_intent.succeeded → Marca orden como PAID
     * - payment_intent.payment_failed → Marca orden como FAILED
     * - payment_intent.canceled → Marca orden como CANCELED
     *
     * @param payload Cuerpo raw del webhook (JSON sin deserializar)
     * @param signatureHeader Firma HMAC del webhook (header "Stripe-Signature")
     * @return HTTP 200 si procesado correctamente, 401 si firma inválida
     */
    @PostMapping("/stripe")
    @Operation(
        summary = "Recibir webhook de Stripe",
        description = "Endpoint para recibir eventos de Stripe. Valida la firma HMAC para garantizar autenticidad. " +
                      "Stripe enviará eventos cuando el estado del pago cambie (succeeded, failed, canceled)."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Webhook procesado exitosamente"
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Firma de webhook inválida - Posible ataque"
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Orden o pago no encontrado en el webhook"
        )
    })
    public ResponseEntity<Void> handleStripeWebhook(
            @RequestBody String payload,
            @Parameter(
                description = "Firma HMAC del webhook enviada por Stripe",
                required = true,
                example = "t=1234567890,v1=abc123..."
            )
            @RequestHeader("Stripe-Signature") String signatureHeader) {

        logger.info("POST /api/webhooks/stripe - Receiving webhook");

        // El WebhookService valida la firma y procesa el evento
        // Si la firma es inválida, lanza InvalidWebhookSignatureException (HTTP 401)
        webhookService.processStripeWebhook(payload, signatureHeader);

        logger.info("Webhook processed successfully");

        // Stripe espera HTTP 200 para confirmar recepción
        return ResponseEntity.ok().build();
    }
}

