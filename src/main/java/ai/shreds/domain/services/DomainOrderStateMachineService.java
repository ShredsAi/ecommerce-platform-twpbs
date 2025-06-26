package ai.shreds.domain.services;

import ai.shreds.domain.entities.DomainOrderEntity;
import ai.shreds.domain.ports.DomainInputPortOrderStateTransition;
import ai.shreds.shared.enums.SharedOrderStatusEnum;
import ai.shreds.domain.exceptions.DomainInvalidStateTransitionException;
import org.springframework.stereotype.Service;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Domain service implementing order state machine logic.
 * This service validates and executes order status transitions according to business rules.
 */
@Service
public class DomainOrderStateMachineService implements DomainInputPortOrderStateTransition {

    // Valid state transitions map
    private static final Map<SharedOrderStatusEnum, Set<SharedOrderStatusEnum>> VALID_TRANSITIONS = Map.of(
        SharedOrderStatusEnum.PENDING, Set.of(
            SharedOrderStatusEnum.CONFIRMED, 
            SharedOrderStatusEnum.CANCELLED
        ),
        SharedOrderStatusEnum.CONFIRMED, Set.of(
            SharedOrderStatusEnum.PAID, 
            SharedOrderStatusEnum.CANCELLED
        ),
        SharedOrderStatusEnum.PAID, Set.of(
            SharedOrderStatusEnum.PROCESSING, 
            SharedOrderStatusEnum.CANCELLED
        ),
        SharedOrderStatusEnum.PROCESSING, Set.of(
            SharedOrderStatusEnum.SHIPPED, 
            SharedOrderStatusEnum.CANCELLED
        ),
        SharedOrderStatusEnum.SHIPPED, Set.of(
            SharedOrderStatusEnum.DELIVERED
        ),
        SharedOrderStatusEnum.DELIVERED, Set.of(
            SharedOrderStatusEnum.COMPLETED
        ),
        SharedOrderStatusEnum.COMPLETED, Set.of(
            // Terminal state - no transitions allowed
        ),
        SharedOrderStatusEnum.CANCELLED, Set.of(
            // Terminal state - no transitions allowed
        )
    );

    @Override
    public boolean validateTransition(SharedOrderStatusEnum currentStatus, SharedOrderStatusEnum newStatus) {
        if (currentStatus == null || newStatus == null) {
            return false;
        }
        
        if (currentStatus == newStatus) {
            return false; // No transition to same state
        }
        
        Set<SharedOrderStatusEnum> validNextStates = VALID_TRANSITIONS.get(currentStatus);
        return validNextStates != null && validNextStates.contains(newStatus);
    }

    @Override
    public DomainOrderEntity transitionOrder(DomainOrderEntity order, SharedOrderStatusEnum newStatus) {
        if (order == null) {
            throw new IllegalArgumentException("Order cannot be null");
        }
        if (newStatus == null) {
            throw new IllegalArgumentException("New status cannot be null");
        }
        
        SharedOrderStatusEnum currentStatus = order.getOrderStatus();
        
        if (!validateTransition(currentStatus, newStatus)) {
            throw new DomainInvalidStateTransitionException(currentStatus.name(), newStatus.name());
        }
        
        // Apply business rules for specific transitions
        enforceStateRules(order, newStatus);
        
        // Execute the transition
        order.transitionTo(newStatus);
        
        return order;
    }

    @Override
    public List<SharedOrderStatusEnum> getNextValidStatuses(SharedOrderStatusEnum currentStatus) {
        if (currentStatus == null) {
            return Arrays.asList();
        }
        
        Set<SharedOrderStatusEnum> validNextStates = VALID_TRANSITIONS.get(currentStatus);
        return validNextStates != null ? validNextStates.stream().toList() : Arrays.asList();
    }

    /**
     * Enforces business rules when transitioning to specific states.
     */
    private void enforceStateRules(DomainOrderEntity order, SharedOrderStatusEnum newStatus) {
        switch (newStatus) {
            case CONFIRMED:
                // Order must have items and valid addresses
                if (order.getOrderItems() == null || order.getOrderItems().isEmpty()) {
                    throw new DomainInvalidStateTransitionException(
                        order.getOrderStatus().name(), 
                        newStatus.name(),
                        "Order must have items to be confirmed"
                    );
                }
                if (order.getBillingAddress() == null || order.getShippingAddress() == null) {
                    throw new DomainInvalidStateTransitionException(
                        order.getOrderStatus().name(), 
                        newStatus.name(),
                        "Order must have billing and shipping addresses"
                    );
                }
                break;
                
            case PAID:
                // Payment validation would be enforced here
                if (order.getTotalAmount() == null || order.getTotalAmount().getValue().doubleValue() <= 0) {
                    throw new DomainInvalidStateTransitionException(
                        order.getOrderStatus().name(), 
                        newStatus.name(),
                        "Order must have valid total amount to be marked as paid"
                    );
                }
                break;
                
            case PROCESSING:
                // Inventory allocation rules
                break;
                
            case SHIPPED:
                // Shipping arrangement rules
                break;
                
            case DELIVERED:
                // Delivery confirmation rules
                break;
                
            case COMPLETED:
                // Final completion rules
                break;
                
            case CANCELLED:
                // Cancellation rules - can be cancelled until shipped
                if (order.getOrderStatus() == SharedOrderStatusEnum.SHIPPED ||
                    order.getOrderStatus() == SharedOrderStatusEnum.DELIVERED ||
                    order.getOrderStatus() == SharedOrderStatusEnum.COMPLETED) {
                    throw new DomainInvalidStateTransitionException(
                        order.getOrderStatus().name(), 
                        newStatus.name(),
                        "Cannot cancel order that has already been shipped or delivered"
                    );
                }
                break;
        }
    }
}