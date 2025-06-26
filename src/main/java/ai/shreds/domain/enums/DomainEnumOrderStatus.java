package ai.shreds.domain.enums;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Represents the possible statuses for an Order in the domain model.
 * Encapsulates business rules about allowed status transitions and operations
 * specific to orders as a whole.
 */
public enum DomainEnumOrderStatus {
    /**
     * The order has been created but not yet confirmed.
     * Initial state for newly created orders.
     */
    PENDING("Pending", "Order has been created but not yet confirmed"),
    
    /**
     * The order has been confirmed by the customer but payment has not been processed.
     * Transition from PENDING when customer confirms the order.
     */
    CONFIRMED("Confirmed", "Order has been confirmed by the customer"),
    
    /**
     * The order has been paid for but not yet started processing.
     * Transition from CONFIRMED when payment is successful.
     */
    PAID("Paid", "Payment has been successfully processed"),
    
    /**
     * The order is being processed for fulfillment.
     * Transition from PAID when warehouse starts preparing the order.
     */
    PROCESSING("Processing", "Order is being prepared for shipping"),
    
    /**
     * The order has been shipped and is in transit.
     * Transition from PROCESSING when order leaves the warehouse.
     */
    SHIPPED("Shipped", "Order has been shipped and is in transit"),
    
    /**
     * The order has been delivered to the customer.
     * Transition from SHIPPED when delivery is confirmed.
     */
    DELIVERED("Delivered", "Order has been delivered to the customer"),
    
    /**
     * The order is considered complete (all items delivered, return period expired).
     * Transition from DELIVERED after return period expires.
     */
    COMPLETED("Completed", "Order has been completed successfully"),
    
    /**
     * The order has been cancelled.
     * Terminal state - can transition from PENDING, CONFIRMED, PAID, or PROCESSING.
     */
    CANCELLED("Cancelled", "Order has been cancelled"),
    
    /**
     * A refund has been processed for the order (either partial or full).
     * Can transition from CANCELLED or can be a marker for partial refunds.
     */
    REFUNDED("Refunded", "A refund has been processed for this order"),
    
    /**
     * Some or all items in the order have been returned.
     * Transition from DELIVERED when returns are processed.
     */
    RETURNED("Returned", "Some or all items have been returned");
    
    private final String displayName;
    private final String description;
    
    // Define valid state transitions
    private static final Set<Transition> VALID_TRANSITIONS;
    
    static {
        Set<Transition> transitions = new HashSet<>();
        // Initial transitions
        transitions.add(new Transition(PENDING, CONFIRMED));
        transitions.add(new Transition(PENDING, CANCELLED));
        
        // Standard fulfillment flow
        transitions.add(new Transition(CONFIRMED, PAID));
        transitions.add(new Transition(CONFIRMED, CANCELLED));
        transitions.add(new Transition(PAID, PROCESSING));
        transitions.add(new Transition(PAID, CANCELLED));
        transitions.add(new Transition(PROCESSING, SHIPPED));
        transitions.add(new Transition(PROCESSING, CANCELLED));
        transitions.add(new Transition(SHIPPED, DELIVERED));
        transitions.add(new Transition(DELIVERED, COMPLETED));
        
        // Post-delivery flows
        transitions.add(new Transition(DELIVERED, RETURNED));
        transitions.add(new Transition(RETURNED, REFUNDED));
        transitions.add(new Transition(RETURNED, COMPLETED));
        
        // Cancellation->Refund flow
        transitions.add(new Transition(CANCELLED, REFUNDED));
        
        VALID_TRANSITIONS = Collections.unmodifiableSet(transitions);
    }
    
    DomainEnumOrderStatus(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }
    
    /**
     * Get a human-readable display name for the status.
     * 
     * @return the display name
     */
    public String getDisplayName() {
        return displayName;
    }
    
    /**
     * Get a description of what this status means.
     * 
     * @return the status description
     */
    public String getDescription() {
        return description;
    }
    
    /**
     * Check if a transition from this status to the given status is allowed.
     * 
     * @param toStatus the target status
     * @return true if the transition is allowed, false otherwise
     */
    public boolean canTransitionTo(DomainEnumOrderStatus toStatus) {
        return VALID_TRANSITIONS.contains(new Transition(this, toStatus));
    }
    
    /**
     * Check if this status allows the order to be cancelled.
     * 
     * @return true if cancellation is allowed, false otherwise
     */
    public boolean canBeCancelled() {
        // Orders can be cancelled if they haven't shipped yet
        return this == PENDING || this == CONFIRMED || this == PAID || this == PROCESSING;
    }
    
    /**
     * Check if this status allows items to be returned.
     * 
     * @return true if returns are allowed, false otherwise
     */
    public boolean canHaveReturns() {
        // Returns are only allowed for delivered orders
        return this == DELIVERED || this == RETURNED;
    }
    
    /**
     * Check if this is a terminal status (no further transitions expected).
     * 
     * @return true if this is a terminal status, false otherwise
     */
    public boolean isTerminalStatus() {
        return this == COMPLETED || this == CANCELLED || this == REFUNDED;
    }
    
    /**
     * Check if the order is in a fulfillment process status.
     * 
     * @return true if the order is being fulfilled
     */
    public boolean isInFulfillmentProcess() {
        return this == PROCESSING || this == SHIPPED;
    }
    
    /**
     * Check if the order has been delivered.
     * 
     * @return true if the order has been delivered
     */
    public boolean isDelivered() {
        return this == DELIVERED || this == RETURNED || this == COMPLETED;
    }
    
    /**
     * Get all possible statuses that this status can transition to.
     * 
     * @return an array of allowed next statuses
     */
    public DomainEnumOrderStatus[] getPossibleNextStatuses() {
        return Arrays.stream(values())
            .filter(status -> canTransitionTo(status))
            .toArray(DomainEnumOrderStatus[]::new);
    }
    
    /**
     * Get a status by its display name.
     * 
     * @param displayName the display name to look up
     * @return the matching status, or null if not found
     */
    public static DomainEnumOrderStatus fromDisplayName(String displayName) {
        for (DomainEnumOrderStatus status : values()) {
            if (status.displayName.equals(displayName)) {
                return status;
            }
        }
        return null;
    }
    
    /**
     * Convert a string representation of the status to the enum constant.
     * This is case-insensitive for more flexible handling of external inputs.
     * 
     * @param statusName the status name (case-insensitive)
     * @return the matching status, or null if not found
     */
    public static DomainEnumOrderStatus fromString(String statusName) {
        if (statusName == null || statusName.isEmpty()) {
            return null;
        }
        
        try {
            return DomainEnumOrderStatus.valueOf(statusName.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
    
    /**
     * Helper class to represent a status transition.
     */
    private static class Transition {
        private final DomainEnumOrderStatus from;
        private final DomainEnumOrderStatus to;
        
        public Transition(DomainEnumOrderStatus from, DomainEnumOrderStatus to) {
            this.from = from;
            this.to = to;
        }
        
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Transition that = (Transition) o;
            return from == that.from && to == that.to;
        }
        
        @Override
        public int hashCode() {
            return 31 * from.hashCode() + to.hashCode();
        }
    }
}