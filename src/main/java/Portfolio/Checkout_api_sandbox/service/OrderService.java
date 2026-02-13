package Portfolio.Checkout_api_sandbox.service;

import Portfolio.Checkout_api_sandbox.dto.request.CreateOrderRequest;
import Portfolio.Checkout_api_sandbox.dto.response.OrderResponse;
import Portfolio.Checkout_api_sandbox.exception.OrderNotFoundException;
import Portfolio.Checkout_api_sandbox.mapper.OrderMapper;
import Portfolio.Checkout_api_sandbox.model.OrderEntity;
import Portfolio.Checkout_api_sandbox.model.OrderStatus;
import Portfolio.Checkout_api_sandbox.repository.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Service para gestionar órdenes.
 * Maneja la creación, consulta y actualización de órdenes.
 */
@Service
public class OrderService {

    private static final Logger logger = LoggerFactory.getLogger(OrderService.class);

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderMapper orderMapper;

    /**
     * Crea una nueva orden con items y calcula los totales.
     *
     * @param request Datos de la orden (moneda e items)
     * @return OrderResponse con la orden creada
     */
    @Transactional
    public OrderResponse createOrder(CreateOrderRequest request) {
        logger.info("Creating new order - Currency: {}, Items count: {}",
                    request.getCurrency(), request.getItems().size());

        // Convertir DTO a Entity (el mapper calcula subtotales y total)
        OrderEntity order = orderMapper.toEntity(request);

        // Guardar en base de datos
        OrderEntity savedOrder = orderRepository.save(order);

        logger.info("Order created successfully - ID: {}, Total: {} {}",
                    savedOrder.getId(), savedOrder.getTotalAmountMinor(), savedOrder.getCurrency());

        // Convertir Entity a Response DTO
        return orderMapper.toResponse(savedOrder);
    }

    /**
     * Obtiene una orden por su ID.
     *
     * @param orderId UUID de la orden
     * @return OrderResponse con los datos de la orden
     * @throws OrderNotFoundException si la orden no existe
     */
    @Transactional(readOnly = true)
    public OrderResponse getOrder(UUID orderId) {
        logger.debug("Fetching order with ID: {}", orderId);

        OrderEntity order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));

        return orderMapper.toResponse(order);
    }

    /**
     * Actualiza el estado de una orden.
     * Usado internamente por CheckoutService y WebhookService.
     *
     * @param orderId UUID de la orden
     * @param newStatus Nuevo estado
     */
    @Transactional
    public void updateOrderStatus(UUID orderId, OrderStatus newStatus) {
        logger.info("Updating order {} status to {}", orderId, newStatus);

        OrderEntity order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));

        OrderStatus oldStatus = order.getStatus();
        order.setStatus(newStatus);
        orderRepository.save(order);

        logger.info("Order {} status updated: {} -> {}", orderId, oldStatus, newStatus);
    }

    /**
     * Busca una orden por ID (uso interno).
     *
     * @param orderId UUID de la orden
     * @return OrderEntity
     * @throws OrderNotFoundException si no existe
     */
    @Transactional(readOnly = true)
    public OrderEntity findOrderById(UUID orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
    }
}

