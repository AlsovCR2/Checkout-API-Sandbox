package Portfolio.Checkout_api_sandbox.exception;

import java.util.UUID;

/**
 * Excepción lanzada cuando no se encuentra una orden por su ID.
 * Retorna HTTP 404 Not Found.
 */
public class OrderNotFoundException extends RuntimeException {

    public OrderNotFoundException(UUID orderId) {
        super("Orden no encontrada con ID: " + orderId);
    }

    public OrderNotFoundException(String message) {
        super(message);
    }
}

