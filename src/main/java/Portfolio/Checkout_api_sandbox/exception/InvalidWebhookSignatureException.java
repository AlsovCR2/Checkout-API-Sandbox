package Portfolio.Checkout_api_sandbox.exception;

/**
 * Excepción lanzada cuando la firma de un webhook de Stripe es inválida.
 * Esto previene el procesamiento de webhooks no auténticos.
 * Retorna HTTP 401 Unauthorized.
 */
public class InvalidWebhookSignatureException extends RuntimeException {

    public InvalidWebhookSignatureException(String message) {
        super(message);
    }

    public InvalidWebhookSignatureException(String message, Throwable cause) {
        super(message, cause);
    }
}

