package ai.shreds.domain.services;

import ai.shreds.domain.ports.DomainInputPortEligibilityChecker;
import ai.shreds.domain.value_objects.DomainCancellationWindowValue;
import ai.shreds.domain.value_objects.DomainReturnPeriodValue;
import ai.shreds.shared.enums.SharedCancellationReasonEnum;
import ai.shreds.shared.dtos.SharedOrderSnapshotDTO;
import ai.shreds.shared.dtos.SharedOrderItemDTO;

import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * Domain service implementing eligibility checking business logic.
 * Contains business rules for determining cancellation and return eligibility.
 */
@Service
public class DomainEligibilityChecker implements DomainInputPortEligibilityChecker {
    
    private final DomainCancellationWindowValue cancellationWindow;
    private final DomainReturnPeriodValue returnPeriod;
    
    // Business rule constants
    private static final String ORDER_STATUS_SHIPPED = "SHIPPED";
    private static final String ORDER_STATUS_DELIVERED = "DELIVERED";
    private static final String ORDER_STATUS_RETURNED = "RETURNED";
    private static final String ORDER_STATUS_CANCELLED = "CANCELLED";
    private static final String PAYMENT_STATUS_PAID = "PAID";
    private static final String PAYMENT_STATUS_REFUNDED = "REFUNDED";
    
    public DomainEligibilityChecker(
        DomainCancellationWindowValue cancellationWindow,
        DomainReturnPeriodValue returnPeriod
    ) {
        this.cancellationWindow = Objects.requireNonNull(cancellationWindow, "Cancellation window cannot be null");
        this.returnPeriod = Objects.requireNonNull(returnPeriod, "Return period cannot be null");
    }
    
    /**
     * Constructor with default values.
     */
    public DomainEligibilityChecker() {
        this.cancellationWindow = new DomainCancellationWindowValue(2); // 2 hours before dispatch
        this.returnPeriod = new DomainReturnPeriodValue(false); // Not holiday period
    }
    
    @Override
    public boolean isCancellationAllowed(SharedOrderSnapshotDTO orderSnapshot, SharedCancellationReasonEnum reason) {
        Objects.requireNonNull(orderSnapshot, "Order snapshot cannot be null");
        Objects.requireNonNull(reason, "Cancellation reason cannot be null");
        
        // Fraud detection can override all rules
        if (checkFraudOverride(reason)) {
            return true;
        }
        
        // Check order status
        if (!checkOrderStatus(orderSnapshot.getOrderStatus())) {
            return false;
        }
        
        // Check payment status
        if (!checkPaymentStatus(orderSnapshot.getPaymentStatus())) {
            return false;
        }
        
        // Check time window
        if (!checkTimeWindow(orderSnapshot.getOrderDate())) {
            return false;
        }
        
        return true;
    }
    
    @Override
    public boolean isReturnAllowed(SharedOrderSnapshotDTO orderSnapshot, List<SharedOrderItemDTO> items) {
        Objects.requireNonNull(orderSnapshot, "Order snapshot cannot be null");
        Objects.requireNonNull(items, "Items cannot be null");
        
        // Order must be delivered
        if (!ORDER_STATUS_DELIVERED.equals(orderSnapshot.getOrderStatus())) {
            return false;
        }
        
        // Check return period
        if (orderSnapshot.getDeliveryDate() != null) {
            if (!returnPeriod.isWithinReturnPeriod(orderSnapshot.getDeliveryDate())) {
                return false;
            }
        } else {
            // If no delivery date, use order date plus estimated delivery time
            LocalDateTime estimatedDelivery = orderSnapshot.getOrderDate().plusDays(7); // Simplified
            if (!returnPeriod.isWithinReturnPeriod(estimatedDelivery)) {
                return false;
            }
        }
        
        // Check if all items are returnable
        for (SharedOrderItemDTO item : items) {
            if (!item.getIsReturnable()) {
                return false;
            }
        }
        
        // Check payment status
        if (!PAYMENT_STATUS_PAID.equals(orderSnapshot.getPaymentStatus())) {
            return false;
        }
        
        return true;
    }
    
    @Override
    public String getCancellationRejectionReason(SharedOrderSnapshotDTO orderSnapshot) {
        Objects.requireNonNull(orderSnapshot, "Order snapshot cannot be null");
        
        if (!checkOrderStatus(orderSnapshot.getOrderStatus())) {
            return "Order status " + orderSnapshot.getOrderStatus() + " does not allow cancellation";
        }
        
        if (!checkPaymentStatus(orderSnapshot.getPaymentStatus())) {
            return "Payment status " + orderSnapshot.getPaymentStatus() + " does not allow cancellation";
        }
        
        if (!checkTimeWindow(orderSnapshot.getOrderDate())) {
            return "Cancellation window has expired. Order was placed too long ago or is too close to shipping";
        }
        
        return null; // No rejection reason
    }
    
    @Override
    public String getReturnRejectionReason(SharedOrderSnapshotDTO orderSnapshot) {
        Objects.requireNonNull(orderSnapshot, "Order snapshot cannot be null");
        
        if (!ORDER_STATUS_DELIVERED.equals(orderSnapshot.getOrderStatus())) {
            return "Order must be delivered before return is allowed. Current status: " + orderSnapshot.getOrderStatus();
        }
        
        if (orderSnapshot.getDeliveryDate() != null) {
            if (!returnPeriod.isWithinReturnPeriod(orderSnapshot.getDeliveryDate())) {
                return "Return period has expired. Returns must be requested within " + returnPeriod.getEffectiveDays() + " days of delivery";
            }
        }
        
        if (!PAYMENT_STATUS_PAID.equals(orderSnapshot.getPaymentStatus())) {
            return "Payment status " + orderSnapshot.getPaymentStatus() + " does not allow returns";
        }
        
        return null; // No rejection reason
    }
    
    /**
     * Check if the order status allows cancellation.
     */
    private boolean checkOrderStatus(String status) {
        if (status == null) {
            return false;
        }
        
        // Orders in these statuses cannot be cancelled
        return !ORDER_STATUS_SHIPPED.equals(status) && 
               !ORDER_STATUS_DELIVERED.equals(status) && 
               !ORDER_STATUS_RETURNED.equals(status) && 
               !ORDER_STATUS_CANCELLED.equals(status);
    }
    
    /**
     * Check if the payment status allows cancellation.
     */
    private boolean checkPaymentStatus(String paymentStatus) {
        if (paymentStatus == null) {
            return false;
        }
        
        // Only paid orders can be cancelled (for refund purposes)
        return PAYMENT_STATUS_PAID.equals(paymentStatus);
    }
    
    /**
     * Check if the cancellation is within the allowed time window.
     */
    private boolean checkTimeWindow(LocalDateTime orderDate) {
        if (orderDate == null) {
            return false;
        }
        
        return cancellationWindow.isWindowOpenForOrder(orderDate, LocalDateTime.now());
    }
    
    /**
     * Check if fraud detection reason overrides other rules.
     */
    private boolean checkFraudOverride(SharedCancellationReasonEnum reason) {
        return reason == SharedCancellationReasonEnum.FRAUD_DETECTION ||
               reason == SharedCancellationReasonEnum.INVENTORY_FAILURE ||
               reason == SharedCancellationReasonEnum.PAYMENT_FAILURE;
    }
}