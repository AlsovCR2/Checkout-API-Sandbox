package Portfolio.Checkout_api_sandbox.controller;

import Portfolio.Checkout_api_sandbox.dto.request.CheckoutRequest;
import Portfolio.Checkout_api_sandbox.dto.response.CheckoutResponse;
import Portfolio.Checkout_api_sandbox.service.CheckoutService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controller REST para gestionar el proceso de checkout.
 * Requiere el header Idempotency-Key para prevenir pagos duplicados.
 */
@RestController
@RequestMapping("/api/checkout")
@Tag(name = "Checkout", description = "API para iniciar el proceso de pago")
public class CheckoutController {

    private static final Logger logger = LoggerFactory.getLogger(CheckoutController.class);

    @Autowired
    private CheckoutService checkoutService;

    /**
     * Inicia el proceso de checkout para una orden.
     * Crea un Payment Intent en Stripe y retorna el client_secret.
     *
     * IMPORTANTE: Requiere el header "Idempotency-Key" para prevenir pagos duplicados.
     *
     * @param request Datos del checkout (orderId y provider)
     * @param idempotencyKey Clave única para idempotencia (header obligatorio)
     * @return CheckoutResponse con el client_secret para el frontend (HTTP 200)
     */
    @PostMapping
    @Operation(
        summary = "Iniciar checkout",
        description = "Inicia el proceso de pago creando un Payment Intent en Stripe. " +
                      "Retorna el client_secret necesario para completar el pago en el frontend. " +
                      "Requiere header 'Idempotency-Key' para prevenir pagos duplicados."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Checkout iniciado exitosamente",
            content = @Content(schema = @Schema(implementation = CheckoutResponse.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Estado de orden inválido o datos incorrectos"
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Orden no encontrada"
        ),
        @ApiResponse(
            responseCode = "409",
            description = "Conflicto de idempotencia - Esta clave ya fue usada"
        ),
        @ApiResponse(
            responseCode = "502",
            description = "Error al comunicarse con Stripe"
        )
    })
    public ResponseEntity<CheckoutResponse> initiateCheckout(
            @Valid @RequestBody CheckoutRequest request,
            @Parameter(
                description = "Clave única de idempotencia (UUID recomendado). Previene pagos duplicados si el cliente reintenta.",
                required = true,
                example = "7f0f3f6b-2bce-4a2f-bb36-1234567890ab"
            )
            @RequestHeader("Idempotency-Key") String idempotencyKey) {

        logger.info("POST /api/checkout - Order: {}, Idempotency-Key: {}",
                    request.getOrderId(), idempotencyKey);

        // Validar que la clave de idempotencia no esté vacía
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new IllegalArgumentException("El header 'Idempotency-Key' es obligatorio");
        }

        CheckoutResponse response = checkoutService.initiateCheckout(request, idempotencyKey);

        logger.info("Checkout initiated successfully - Payment ID: {}, Order: {}",
                    response.getPaymentId(), response.getOrderId());

        return ResponseEntity.ok(response);
    }
}

