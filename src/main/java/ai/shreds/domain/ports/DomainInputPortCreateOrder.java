package ai.shreds.domain.ports;

import ai.shreds.application.dtos.ApplicationInventoryItemDTO;
import ai.shreds.application.dtos.ApplicationPricingResponseDTO;
import ai.shreds.shared.dtos.SharedAddressDTO;
import ai.shreds.shared.dtos.SharedPaymentMethodDTO;
import ai.shreds.domain.value_objects.DomainOrderAggregate;

import java.util.List;

/**
 * Domain input port for creating orders.
 * This port is implemented by domain services and called by application services.
 */
public interface DomainInputPortCreateOrder {
    
    /**
     * Creates an order from the provided input data.
     *
     * @param cartId the unique cart identifier for idempotency
     * @param customerId the customer placing the order
     * @param items the list of items to be ordered
     * @param billingAddress the billing address
     * @param shippingAddress the shipping address
     * @param pricing the calculated pricing information
     * @param paymentMethod the payment method details
     * @return the created order aggregate
     */
    DomainOrderAggregate execute(String cartId, 
                                String customerId, 
                                List<ApplicationInventoryItemDTO> items, 
                                SharedAddressDTO billingAddress, 
                                SharedAddressDTO shippingAddress, 
                                ApplicationPricingResponseDTO pricing, 
                                SharedPaymentMethodDTO paymentMethod);
}