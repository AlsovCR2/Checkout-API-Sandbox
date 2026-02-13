package Portfolio.Checkout_api_sandbox.exception;

/**
 * Excepción lanzada cuando una orden no puede ser procesada por estar en un estado inválido.
 * Por ejemplo, intentar hacer checkout de una orden ya pagada o cancelada.
 * Retorna HTTP 400 Bad Request.
 */
public class InvalidOrderStateException extends RuntimeException {

    public InvalidOrderStateException(String message) {
        super(message);
    }
}

