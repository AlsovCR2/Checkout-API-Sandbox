package Portfolio.Checkout_api_sandbox.mapper;

import Portfolio.Checkout_api_sandbox.dto.response.CheckoutResponse;
import Portfolio.Checkout_api_sandbox.model.PaymentEntity;
import org.springframework.stereotype.Component;

/**
 * Mapper para convertir entre PaymentEntity y CheckoutResponse.
 */
@Component
public class PaymentMapper {

    /**
     * Convierte PaymentEntity a CheckoutResponse.
     * Incluye el client_secret necesario para completar el pago en el frontend.
     */
    public CheckoutResponse toCheckoutResponse(PaymentEntity payment) {
        return new CheckoutResponse(
                payment.getOrder().getId(),
                payment.getId(),
                payment.getProvider().name(),
                payment.getClientSecret(),
                payment.getOrder().getStatus()
        );
    }
}

