package Portfolio.Checkout_api_sandbox.mapper;

import Portfolio.Checkout_api_sandbox.dto.request.CreateOrderRequest;
import Portfolio.Checkout_api_sandbox.dto.request.OrderItemRequest;
import Portfolio.Checkout_api_sandbox.dto.response.OrderItemResponse;
import Portfolio.Checkout_api_sandbox.dto.response.OrderResponse;
import Portfolio.Checkout_api_sandbox.model.OrderEntity;
import Portfolio.Checkout_api_sandbox.model.OrderItemEntity;
import Portfolio.Checkout_api_sandbox.model.OrderStatus;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Mapper para convertir entre OrderEntity y sus DTOs.
 * Maneja la conversión bidireccional y cálculos de totales.
 */
@Component
public class OrderMapper {

    /**
     * Convierte CreateOrderRequest a OrderEntity.
     * Calcula subtotales y total automáticamente.
     */
    public OrderEntity toEntity(CreateOrderRequest request) {
        OrderEntity order = new OrderEntity();
        order.setCurrency(request.getCurrency());
        order.setStatus(OrderStatus.CREATED);

        // Convertir items y calcular totales
        long totalAmount = 0L;
        for (OrderItemRequest itemRequest : request.getItems()) {
            OrderItemEntity item = toItemEntity(itemRequest);
            order.addItem(item);
            totalAmount += item.getSubtotalMinor();
        }

        order.setTotalAmountMinor(totalAmount);
        return order;
    }

    /**
     * Convierte OrderItemRequest a OrderItemEntity.
     * Calcula el subtotal (precio × cantidad).
     */
    private OrderItemEntity toItemEntity(OrderItemRequest request) {
        OrderItemEntity item = new OrderItemEntity();
        item.setName(request.getName());
        item.setUnitPriceMinor(request.getUnitPriceMinor());
        item.setQuantity(request.getQuantity());
        item.setSubtotalMinor(request.getUnitPriceMinor() * request.getQuantity());
        return item;
    }

    /**
     * Convierte OrderEntity a OrderResponse.
     * Incluye todos los items con sus subtotales.
     */
    public OrderResponse toResponse(OrderEntity entity) {
        List<OrderItemResponse> itemResponses = entity.getItems().stream()
                .map(this::toItemResponse)
                .collect(Collectors.toList());

        return new OrderResponse(
                entity.getId(),
                entity.getStatus(),
                entity.getCurrency(),
                entity.getTotalAmountMinor(),
                itemResponses,
                entity.getCreatedAt()
        );
    }

    /**
     * Convierte OrderItemEntity a OrderItemResponse.
     */
    private OrderItemResponse toItemResponse(OrderItemEntity entity) {
        return new OrderItemResponse(
                entity.getName(),
                entity.getUnitPriceMinor(),
                entity.getQuantity(),
                entity.getSubtotalMinor()
        );
    }
}

