package Portfolio.Checkout_api_sandbox.dto.response;

/**
 * DTO para representar un item en la respuesta de una orden.
 * Incluye el subtotal calculado.
 */
public class OrderItemResponse {

    private String name;
    private Long unitPriceMinor;
    private Integer quantity;
    private Long subtotalMinor;

    // Constructores
    public OrderItemResponse() {
    }

    public OrderItemResponse(String name, Long unitPriceMinor, Integer quantity, Long subtotalMinor) {
        this.name = name;
        this.unitPriceMinor = unitPriceMinor;
        this.quantity = quantity;
        this.subtotalMinor = subtotalMinor;
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

    public Long getSubtotalMinor() {
        return subtotalMinor;
    }

    public void setSubtotalMinor(Long subtotalMinor) {
        this.subtotalMinor = subtotalMinor;
    }
}

