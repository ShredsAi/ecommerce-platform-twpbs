package ai.shreds.domain.services;

import ai.shreds.application.dtos.ApplicationInventoryItemDTO;
import ai.shreds.application.dtos.ApplicationPricingResponseDTO;
import ai.shreds.application.dtos.ApplicationItemPricingDTO;
import ai.shreds.domain.entities.*;
import ai.shreds.domain.value_objects.DomainOrderAggregate;
import ai.shreds.domain.exceptions.DomainValidationException;
import ai.shreds.shared.dtos.SharedPaymentMethodDTO;
import ai.shreds.shared.enums.SharedOrderStatusEnum;
import ai.shreds.shared.enums.SharedOrderItemStatusEnum;
import ai.shreds.shared.enums.SharedPaymentStatusEnum;
import ai.shreds.shared.enums.SharedShippingStatusEnum;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Domain service responsible for creating order aggregates.
 */
@Service
public class DomainServiceOrderFactory {
    
    private static final AtomicLong orderNumberSequence = new AtomicLong(1);
    private static final String ORDER_NUMBER_PREFIX = "ORD-";
    
    /**
     * Creates a complete order aggregate from the provided inputs.
     *
     * @param customerId the customer ID
     * @param cartId the cart ID for correlation
     * @param items the items to order
     * @param pricing the pricing information
     * @param billingAddress the billing address entity
     * @param shippingAddress the shipping address entity
     * @param paymentMethod the payment method details
     * @return the created order aggregate
     */
    public DomainOrderAggregate create(String customerId, 
                                      String cartId, 
                                      List<ApplicationInventoryItemDTO> items, 
                                      ApplicationPricingResponseDTO pricing, 
                                      DomainAddressEntity billingAddress, 
                                      DomainAddressEntity shippingAddress, 
                                      SharedPaymentMethodDTO paymentMethod) {
        
        validateInputs(customerId, cartId, items, pricing, billingAddress, shippingAddress, paymentMethod);
        
        UUID orderId = UUID.randomUUID();
        String orderNumber = generateOrderNumber();
        LocalDateTime now = LocalDateTime.now();
        
        // Create order entity
        DomainOrderEntity order = DomainOrderEntity.builder()
            .orderId(orderId)
            .orderNumber(orderNumber)
            .cartId(cartId)
            .customerId(customerId)
            .orderStatus(SharedOrderStatusEnum.PENDING)
            .orderDate(now)
            .subtotalAmount(pricing.getSubtotal().getAmount())
            .totalAmount(pricing.getTotal().getAmount())
            .currency(pricing.getTotal().getCurrency())
            .billingAddressId(billingAddress.getAddressId())
            .shippingAddressId(shippingAddress.getAddressId())
            .version(0)
            .createdAt(now)
            .updatedAt(now)
            .build();
        
        // Create order items
        List<DomainOrderItemEntity> orderItems = createOrderItems(orderId, items, pricing);
        
        // Create payment details
        DomainPaymentDetailsEntity paymentDetails = createPaymentDetails(orderId, pricing.getTotal().getAmount(), 
            pricing.getTotal().getCurrency(), paymentMethod);
        
        // Create shipping details
        DomainShippingDetailsEntity shippingDetails = createShippingDetails(orderId);
        
        // Build and return aggregate
        return DomainOrderAggregate.builder()
            .order(order)
            .orderItems(orderItems)
            .paymentDetails(paymentDetails)
            .shippingDetails(shippingDetails)
            .billingAddress(billingAddress)
            .shippingAddress(shippingAddress)
            .build();
    }
    
    /**
     * Generates a sequential order number.
     *
     * @return the generated order number
     */
    public String generateOrderNumber() {
        long sequence = orderNumberSequence.getAndIncrement();
        int year = LocalDateTime.now().getYear();
        return String.format("%s%d-%06d", ORDER_NUMBER_PREFIX, year, sequence);
    }
    
    private List<DomainOrderItemEntity> createOrderItems(UUID orderId, 
                                                         List<ApplicationInventoryItemDTO> items, 
                                                         ApplicationPricingResponseDTO pricing) {
        List<DomainOrderItemEntity> orderItems = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        
        for (ApplicationInventoryItemDTO item : items) {
            // Find pricing for this item
            ApplicationItemPricingDTO itemPricing = findItemPricing(item.getProductId(), pricing.getItemBreakdown());
            
            if (itemPricing == null) {
                throw new DomainValidationException("No pricing found for product: " + item.getProductId(), 
                    List.of("Product " + item.getProductId() + " missing from pricing breakdown"));
            }
            
            DomainOrderItemEntity orderItem = DomainOrderItemEntity.builder()
                .orderItemId(UUID.randomUUID())
                .orderId(orderId)
                .productId(item.getProductId())
                .quantity(item.getQuantity())
                .unitPrice(itemPricing.getUnitPrice())
                .totalPrice(itemPricing.getTotalPrice())
                .currency(pricing.getTotal().getCurrency())
                .itemStatus(SharedOrderItemStatusEnum.PENDING)
                .createdAt(now)
                .build();
            
            orderItems.add(orderItem);
        }
        
        return orderItems;
    }
    
    private DomainPaymentDetailsEntity createPaymentDetails(UUID orderId, 
                                                           BigDecimal totalAmount, 
                                                           String currency, 
                                                           SharedPaymentMethodDTO paymentMethod) {
        return DomainPaymentDetailsEntity.builder()
            .paymentId(UUID.randomUUID())
            .orderId(orderId)
            .paymentStatus(SharedPaymentStatusEnum.PENDING)
            .paymentAmount(totalAmount)
            .currency(currency)
            .paymentProvider(paymentMethod.getProvider())
            .createdAt(LocalDateTime.now())
            .build();
    }
    
    private DomainShippingDetailsEntity createShippingDetails(UUID orderId) {
        return DomainShippingDetailsEntity.builder()
            .shippingId(UUID.randomUUID())
            .orderId(orderId)
            .shippingStatus(SharedShippingStatusEnum.PENDING)
            .createdAt(LocalDateTime.now())
            .build();
    }
    
    private ApplicationItemPricingDTO findItemPricing(String productId, List<ApplicationItemPricingDTO> itemBreakdown) {
        return itemBreakdown.stream()
            .filter(pricing -> productId.equals(pricing.getProductId()))
            .findFirst()
            .orElse(null);
    }
    
    private void validateInputs(String customerId, 
                               String cartId, 
                               List<ApplicationInventoryItemDTO> items, 
                               ApplicationPricingResponseDTO pricing, 
                               DomainAddressEntity billingAddress, 
                               DomainAddressEntity shippingAddress, 
                               SharedPaymentMethodDTO paymentMethod) {
        
        List<String> errors = new ArrayList<>();
        
        if (customerId == null || customerId.trim().isEmpty()) {
            errors.add("Customer ID cannot be null or empty");
        }
        
        if (cartId == null || cartId.trim().isEmpty()) {
            errors.add("Cart ID cannot be null or empty");
        }
        
        if (items == null || items.isEmpty()) {
            errors.add("Items list cannot be null or empty");
        }
        
        if (pricing == null) {
            errors.add("Pricing information cannot be null");
        }
        
        if (billingAddress == null) {
            errors.add("Billing address cannot be null");
        }
        
        if (shippingAddress == null) {
            errors.add("Shipping address cannot be null");
        }
        
        if (paymentMethod == null) {
            errors.add("Payment method cannot be null");
        }
        
        if (!errors.isEmpty()) {
            throw new DomainValidationException("Order factory validation failed", errors);
        }
    }
}