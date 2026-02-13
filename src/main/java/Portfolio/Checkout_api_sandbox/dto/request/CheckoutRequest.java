package Portfolio.Checkout_api_sandbox.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * DTO para iniciar el proceso de checkout.
 * Requiere el ID de la orden y el proveedor de pago.
 */
public class CheckoutRequest {

    @NotNull(message = "El ID de la orden es obligatorio")
    private UUID orderId;

    @NotBlank(message = "El proveedor de pago es obligatorio")
    private String provider;

    // Constructores
    public CheckoutRequest() {
    }

    public CheckoutRequest(UUID orderId, String provider) {
        this.orderId = orderId;
        this.provider = provider;
    }

    // Getters y Setters
    public UUID getOrderId() {
        return orderId;
    }

    public void setOrderId(UUID orderId) {
        this.orderId = orderId;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }
}

