package Portfolio.Checkout_api_sandbox.exception;

import java.util.UUID;

/**
 * Excepción lanzada cuando no se encuentra un pago por su ID o identificador externo.
 * Retorna HTTP 404 Not Found.
 */
public class PaymentNotFoundException extends RuntimeException {

    public PaymentNotFoundException(UUID paymentId) {
        super("Pago no encontrado con ID: " + paymentId);
    }

    public PaymentNotFoundException(String message) {
        super(message);
    }
}

