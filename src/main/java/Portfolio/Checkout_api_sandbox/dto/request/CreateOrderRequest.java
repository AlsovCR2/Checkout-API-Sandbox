package Portfolio.Checkout_api_sandbox.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * DTO para crear una nueva orden.
 * Contiene la moneda y la lista de items a comprar.
 */
public class CreateOrderRequest {

    @NotBlank(message = "La moneda es obligatoria")
    @Size(min = 3, max = 3, message = "La moneda debe tener exactamente 3 caracteres (ej: USD, EUR)")
    @Pattern(regexp = "^[A-Z]{3}$", message = "La moneda debe estar en mayúsculas (ej: USD, EUR, MXN)")
    private String currency;

    @NotEmpty(message = "Debe incluir al menos un item")
    @Valid
    private List<OrderItemRequest> items;

    // Constructores
    public CreateOrderRequest() {
    }

    public CreateOrderRequest(String currency, List<OrderItemRequest> items) {
        this.currency = currency;
        this.items = items;
    }

    // Getters y Setters
    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public List<OrderItemRequest> getItems() {
        return items;
    }

    public void setItems(List<OrderItemRequest> items) {
        this.items = items;
    }
}

