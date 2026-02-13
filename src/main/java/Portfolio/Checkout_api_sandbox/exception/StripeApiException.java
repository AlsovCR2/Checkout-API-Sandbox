package Portfolio.Checkout_api_sandbox.exception;

/**
 * Excepción lanzada cuando ocurre un error al comunicarse con la API de Stripe.
 * Encapsula errores de red, errores de la API de Stripe, etc.
 * Retorna HTTP 500 Internal Server Error o HTTP 502 Bad Gateway.
 */
public class StripeApiException extends RuntimeException {

    public StripeApiException(String message) {
        super(message);
    }

    public StripeApiException(String message, Throwable cause) {
        super(message, cause);
    }
}

