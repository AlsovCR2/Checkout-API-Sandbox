package Portfolio.Checkout_api_sandbox.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * DTO para crear un item dentro de una orden.
 * Representa un producto con su precio y cantidad.
 */
public class OrderItemRequest {

    @NotBlank(message = "El nombre del item es obligatorio")
    private String name;

    @NotNull(message = "El precio unitario es obligatorio")
    @Positive(message = "El precio unitario debe ser mayor a 0")
    private Long unitPriceMinor;

    @NotNull(message = "La cantidad es obligatoria")
    @Positive(message = "La cantidad debe ser mayor a 0")
    private Integer quantity;

    // Constructores
    public OrderItemRequest() {
    }

    public OrderItemRequest(String name, Long unitPriceMinor, Integer quantity) {
        this.name = name;
        this.unitPriceMinor = unitPriceMinor;
        this.quantity = quantity;
    }

    // Getters y Setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Long getUnitPriceMinor() {
        return unitPriceMinor;
    }

    public void setUnitPriceMinor(Long unitPriceMinor) {
        this.unitPriceMinor = unitPriceMinor;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }
}

