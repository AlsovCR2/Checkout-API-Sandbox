package Portfolio.Checkout_api_sandbox.controller;

import Portfolio.Checkout_api_sandbox.dto.request.CreateOrderRequest;
import Portfolio.Checkout_api_sandbox.dto.response.OrderResponse;
import Portfolio.Checkout_api_sandbox.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Controller REST para gestionar órdenes.
 * Expone endpoints para crear y consultar órdenes.
 */
@RestController
@RequestMapping("/api/orders")
@Tag(name = "Orders", description = "API para gestión de órdenes")
public class OrderController {

    private static final Logger logger = LoggerFactory.getLogger(OrderController.class);

    @Autowired
    private OrderService orderService;

    /**
     * Crea una nueva orden con items.
     * Calcula automáticamente subtotales y total.
     *
     * @param request Datos de la orden (moneda e items)
     * @return OrderResponse con la orden creada (HTTP 201)
     */
    @PostMapping
    @Operation(
        summary = "Crear una nueva orden",
        description = "Crea una orden con items y calcula automáticamente los totales. El estado inicial es CREATED."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "201",
            description = "Orden creada exitosamente",
            content = @Content(schema = @Schema(implementation = OrderResponse.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Datos de entrada inválidos (validación falló)"
        )
    })
    public ResponseEntity<OrderResponse> createOrder(
            @Valid @RequestBody CreateOrderRequest request) {

        logger.info("POST /api/orders - Creating order with {} items", request.getItems().size());

        OrderResponse response = orderService.createOrder(request);

        logger.info("Order created successfully - ID: {}", response.getOrderId());

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    /**
     * Obtiene una orden por su ID.
     *
     * @param orderId UUID de la orden
     * @return OrderResponse con los datos de la orden (HTTP 200)
     */
    @GetMapping("/{orderId}")
    @Operation(
        summary = "Consultar una orden",
        description = "Obtiene los detalles de una orden existente por su ID."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Orden encontrada",
            content = @Content(schema = @Schema(implementation = OrderResponse.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Orden no encontrada"
        )
    })
    public ResponseEntity<OrderResponse> getOrder(
            @Parameter(description = "UUID de la orden", required = true)
            @PathVariable UUID orderId) {

        logger.info("GET /api/orders/{} - Fetching order", orderId);

        OrderResponse response = orderService.getOrder(orderId);

        return ResponseEntity.ok(response);
    }
}

