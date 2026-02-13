package Portfolio.Checkout_api_sandbox.exception;

/**
 * Excepción lanzada cuando se detecta un conflicto de idempotencia.
 * Ocurre cuando se intenta procesar una solicitud con una clave de idempotencia ya usada.
 * Retorna HTTP 409 Conflict.
 */
public class IdempotencyConflictException extends RuntimeException {

    public IdempotencyConflictException(String idempotencyKey) {
        super("Ya existe una solicitud procesada con la clave de idempotencia: " + idempotencyKey);
    }

    public IdempotencyConflictException(String message, Throwable cause) {
        super(message, cause);
    }
}

