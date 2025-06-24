package ai.shreds.domain.value_objects;

import ai.shreds.domain.entities.*;
import ai.shreds.domain.exceptions.DomainValidationException;
import ai.shreds.domain.exceptions.DomainInvariantViolationException;
import ai.shreds.shared.dtos.SharedOrderCreatedEventDTO;
import ai.shreds.shared.dtos.SharedMoneyDTO;
import ai.shreds.shared.enums.SharedOrderStatusEnum;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Getter
@Builder
public class DomainOrderAggregate {
    private final DomainOrderEntity order;
    private final List<DomainOrderItemEntity> orderItems;
    private final DomainPaymentDetailsEntity paymentDetails;
    private final DomainShippingDetailsEntity shippingDetails;
    private final DomainAddressEntity billingAddress;
    private final DomainAddressEntity shippingAddress;

    public DomainOrderAggregate(DomainOrderEntity order,
                               List<DomainOrderItemEntity> orderItems,
                               DomainPaymentDetailsEntity paymentDetails,
                               DomainShippingDetailsEntity shippingDetails,
                               DomainAddressEntity billingAddress,
                               DomainAddressEntity shippingAddress) {
        this.order = order;
        this.orderItems = orderItems;
        this.paymentDetails = paymentDetails;
        this.shippingDetails = shippingDetails;
        this.billingAddress = billingAddress;
        this.shippingAddress = shippingAddress;
        
        validateInvariants();
    }

    public void calculateTotals() {
        if (orderItems == null || orderItems.isEmpty()) {
            throw new DomainValidationException("Cannot calculate totals without order items",
                List.of("orderItems cannot be null or empty"));
        }

        BigDecimal calculatedSubtotal = orderItems.stream()
            .map(DomainOrderItemEntity::getTotalPrice)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Update order entity with calculated totals
        order.setSubtotalAmount(calculatedSubtotal);
        order.setTotalAmount(calculatedSubtotal); // For now, subtotal = total (no taxes/fees in this version)
        
        // Ensure payment amount matches order total
        if (paymentDetails != null) {
            paymentDetails.setPaymentAmount(calculatedSubtotal);
            paymentDetails.setCurrency(order.getCurrency());
        }
    }

    public void validateInvariants() {
        validateOrderNotNull();
        validateOrderItems();
        validatePaymentDetails();
        validateAddresses();
        validateBusinessRules();
    }

    private void validateOrderNotNull() {
        if (order == null) {
            throw new DomainInvariantViolationException("Order entity cannot be null", 
                "order", null);
        }
    }

    private void validateOrderItems() {
        if (orderItems == null || orderItems.isEmpty()) {
            throw new DomainInvariantViolationException("Order must have at least one item", 
                "orderItems.size", orderItems != null ? orderItems.size() : 0);
        }
        
        if (orderItems.size() > 50) {
            throw new DomainInvariantViolationException("Order cannot have more than 50 items", 
                "orderItems.size", orderItems.size());
        }

        for (DomainOrderItemEntity item : orderItems) {
            if (!item.getOrderId().equals(order.getOrderId())) {
                throw new DomainInvariantViolationException("Order item must belong to the same order", 
                    "orderItem.orderId", item.getOrderId());
            }
        }
    }

    private void validatePaymentDetails() {
        if (paymentDetails == null) {
            throw new DomainInvariantViolationException("Payment details cannot be null", 
                "paymentDetails", null);
        }
        
        if (!paymentDetails.getOrderId().equals(order.getOrderId())) {
            throw new DomainInvariantViolationException("Payment details must belong to the same order", 
                "paymentDetails.orderId", paymentDetails.getOrderId());
        }

        if (order.getTotalAmount() != null && paymentDetails.getPaymentAmount() != null) {
            if (order.getTotalAmount().compareTo(paymentDetails.getPaymentAmount()) != 0) {
                throw new DomainInvariantViolationException("Payment amount must equal order total", 
                    "paymentAmount vs totalAmount", 
                    paymentDetails.getPaymentAmount() + " vs " + order.getTotalAmount());
            }
        }
    }

    private void validateAddresses() {
        if (billingAddress == null) {
            throw new DomainInvariantViolationException("Billing address cannot be null", 
                "billingAddress", null);
        }
        
        if (shippingAddress == null) {
            throw new DomainInvariantViolationException("Shipping address cannot be null", 
                "shippingAddress", null);
        }
        
        if (!billingAddress.getAddressId().equals(order.getBillingAddressId())) {
            throw new DomainInvariantViolationException("Billing address ID must match order reference", 
                "billingAddress.addressId", billingAddress.getAddressId());
        }
        
        if (!shippingAddress.getAddressId().equals(order.getShippingAddressId())) {
            throw new DomainInvariantViolationException("Shipping address ID must match order reference", 
                "shippingAddress.addressId", shippingAddress.getAddressId());
        }
    }

    private void validateBusinessRules() {
        if (order.getTotalAmount() != null && order.getTotalAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new DomainInvariantViolationException("Order total must be greater than zero", 
                "order.totalAmount", order.getTotalAmount());
        }
        
        if (order.getSubtotalAmount() != null && order.getSubtotalAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new DomainInvariantViolationException("Order subtotal must be greater than zero", 
                "order.subtotalAmount", order.getSubtotalAmount());
        }
    }

    public SharedOrderCreatedEventDTO toDTO() {
        return SharedOrderCreatedEventDTO.builder()
            .orderId(order.getOrderId().toString())
            .orderNumber(order.getOrderNumber())
            .customerId(order.getCustomerId())
            .orderStatus(order.getOrderStatus())
            .totalAmount(SharedMoneyDTO.builder()
                .amount(order.getTotalAmount())
                .currency(order.getCurrency())
                .build())
            .itemCount(orderItems.size())
            .timestamp(Instant.now())
            .correlationId(order.getCartId())
            .eventId(UUID.randomUUID().toString())
            .occurredOn(Instant.now())
            .build();
    }

    public boolean isPending() {
        return SharedOrderStatusEnum.PENDING.equals(order.getOrderStatus());
    }

    public boolean isConfirmed() {
        return SharedOrderStatusEnum.CONFIRMED.equals(order.getOrderStatus());
    }

    public int getItemCount() {
        return orderItems != null ? orderItems.size() : 0;
    }

    public DomainMoneyValue getTotalMoney() {
        return new DomainMoneyValue(order.getTotalAmount(), order.getCurrency());
    }

    public DomainOrderIdValue getOrderId() {
        return new DomainOrderIdValue(order.getOrderId());
    }

    public DomainCustomerIdValue getCustomerId() {
        return new DomainCustomerIdValue(order.getCustomerId());
    }
}