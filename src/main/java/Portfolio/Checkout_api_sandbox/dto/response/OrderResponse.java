package Portfolio.Checkout_api_sandbox.dto.response;

import Portfolio.Checkout_api_sandbox.model.OrderStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * DTO para la respuesta al crear o consultar una orden.
 * Incluye el ID, estado, totales y lista de items.
 */
public class OrderResponse {

    private UUID orderId;
    private OrderStatus status;
    private String currency;
    private Long totalAmountMinor;
    private List<OrderItemResponse> items;
    private LocalDateTime createdAt;

    // Constructores
    public OrderResponse() {
    }

    public OrderResponse(UUID orderId, OrderStatus status, String currency, Long totalAmountMinor,
                        List<OrderItemResponse> items, LocalDateTime createdAt) {
        this.orderId = orderId;
        this.status = status;
        this.currency = currency;
        this.totalAmountMinor = totalAmountMinor;
        this.items = items;
        this.createdAt = createdAt;
    }

    // Getters y Setters
    public UUID getOrderId() {
        return orderId;
    }

    public void setOrderId(UUID orderId) {
        this.orderId = orderId;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public Long getTotalAmountMinor() {
        return totalAmountMinor;
    }

    public void setTotalAmountMinor(Long totalAmountMinor) {
        this.totalAmountMinor = totalAmountMinor;
    }

    public List<OrderItemResponse> getItems() {
        return items;
    }

    public void setItems(List<OrderItemResponse> items) {
        this.items = items;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}

