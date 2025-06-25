package ai.shreds.domain.enums;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Represents the possible statuses for an Order Item in the domain model.
 * Encapsulates business rules about allowed status transitions and operations
 * specific to order items.
 */
public enum DomainEnumOrderItemStatus {
    /**
     * The order item has been created but not yet confirmed.
     * Initial state for newly created order items.
     */
    PENDING("Pending", "Item has been added to order but not yet confirmed"),
    
    /**
     * The order item has been confirmed and is ready for processing.
     * Transition from PENDING when order is confirmed.
     */
    CONFIRMED("Confirmed", "Item has been confirmed and payment has been validated"),
    
    /**
     * The order item is being processed for fulfillment.
     * Transition from CONFIRMED when warehouse starts preparing the item.
     */
    PROCESSING("Processing", "Item is being prepared for shipping"),
    
    /**
     * The order item has been shipped and is in transit.
     * Transition from PROCESSING when item leaves the warehouse.
     */
    SHIPPED("Shipped", "Item has been shipped and is in transit"),
    
    /**
     * The order item has been delivered to the customer.
     * Transition from SHIPPED when delivery is confirmed.
     */
    DELIVERED("Delivered", "Item has been delivered to the customer"),
    
    /**
     * The order item has been cancelled.
     * Terminal state - can transition from PENDING, CONFIRMED, or PROCESSING.
     */
    CANCELLED("Cancelled", "Item has been cancelled and will not be processed"),
    
    /**
     * The order item has been returned by the customer.
     * Terminal state - can transition from DELIVERED.
     */
    RETURNED("Returned", "Item has been returned by the customer"),
    
    /**
     * The order item had a refund processed.
     * Terminal state - can transition from CANCELLED or RETURNED.
     */
    REFUNDED("Refunded", "A refund has been processed for this item");
    
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
        transitions.add(new Transition(CONFIRMED, PROCESSING));
        transitions.add(new Transition(CONFIRMED, CANCELLED));
        transitions.add(new Transition(PROCESSING, SHIPPED));
        transitions.add(new Transition(PROCESSING, CANCELLED));
        transitions.add(new Transition(SHIPPED, DELIVERED));
        
        // Post-delivery flows
        transitions.add(new Transition(DELIVERED, RETURNED));
        
        // Refund flows
        transitions.add(new Transition(CANCELLED, REFUNDED));
        transitions.add(new Transition(RETURNED, REFUNDED));
        
        VALID_TRANSITIONS = Collections.unmodifiableSet(transitions);
    }
    
    DomainEnumOrderItemStatus(String displayName, String description) {
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
    public boolean canTransitionTo(DomainEnumOrderItemStatus toStatus) {
        return VALID_TRANSITIONS.contains(new Transition(this, toStatus));
    }
    
    /**
     * Check if this status allows the item to be cancelled.
     * 
     * @return true if cancellation is allowed, false otherwise
     */
    public boolean canBeCancelled() {
        // Items can be cancelled if they haven't shipped yet
        return this == PENDING || this == CONFIRMED || this == PROCESSING;
    }
    
    /**
     * Check if this status allows the item to be returned.
     * 
     * @return true if return is allowed, false otherwise
     */
    public boolean canBeReturned() {
        // Items can only be returned if they have been delivered
        return this == DELIVERED;
    }
    
    /**
     * Check if this is a terminal status (no further transitions expected).
     * 
     * @return true if this is a terminal status, false otherwise
     */
    public boolean isTerminalStatus() {
        return this == CANCELLED || this == RETURNED || this == REFUNDED;
    }
    
    /**
     * Get all possible statuses that this status can transition to.
     * 
     * @return an array of allowed next statuses
     */
    public DomainEnumOrderItemStatus[] getPossibleNextStatuses() {
        return Arrays.stream(values())
            .filter(status -> canTransitionTo(status))
            .toArray(DomainEnumOrderItemStatus[]::new);
    }
    
    /**
     * Get a status by its display name.
     * 
     * @param displayName the display name to look up
     * @return the matching status, or null if not found
     */
    public static DomainEnumOrderItemStatus fromDisplayName(String displayName) {
        for (DomainEnumOrderItemStatus status : values()) {
            if (status.displayName.equals(displayName)) {
                return status;
            }
        }
        return null;
    }
    
    /**
     * Helper class to represent a status transition.
     */
    private static class Transition {
        private final DomainEnumOrderItemStatus from;
        private final DomainEnumOrderItemStatus to;
        
        public Transition(DomainEnumOrderItemStatus from, DomainEnumOrderItemStatus to) {
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