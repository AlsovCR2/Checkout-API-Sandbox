package Portfolio.Checkout_api_sandbox.dto.response;

import Portfolio.Checkout_api_sandbox.model.OrderStatus;

import java.util.UUID;

/**
 * DTO para la respuesta al iniciar un checkout.
 * Incluye el client_secret necesario para completar el pago en el frontend.
 */
public class CheckoutResponse {

    private UUID orderId;
    private UUID paymentId;
    private String provider;
    private String clientSecret;
    private OrderStatus status;

    // Constructores
    public CheckoutResponse() {
    }

    public CheckoutResponse(UUID orderId, UUID paymentId, String provider, String clientSecret, OrderStatus status) {
        this.orderId = orderId;
        this.paymentId = paymentId;
        this.provider = provider;
        this.clientSecret = clientSecret;
        this.status = status;
    }

    // Getters y Setters
    public UUID getOrderId() {
        return orderId;
    }

    public void setOrderId(UUID orderId) {
        this.orderId = orderId;
    }

    public UUID getPaymentId() {
        return paymentId;
    }

    public void setPaymentId(UUID paymentId) {
        this.paymentId = paymentId;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }
}

