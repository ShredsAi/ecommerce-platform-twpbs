package ai.shreds.domain.value_objects;

import java.util.Objects;

/**
 * Domain value object representing an Order ID.
 * Encapsulates validation and business rules related to order identifiers.
 */
public final class DomainValueOrderId {
    
    private final String value;
    
    /**
     * Create a new Order ID value object.
     *
     * @param value the order ID string value
     * @throws IllegalArgumentException if the order ID is invalid
     */
    public DomainValueOrderId(String value) {
        Objects.requireNonNull(value, "Order ID cannot be null");
        
        if (value.trim().isEmpty()) {
            throw new IllegalArgumentException("Order ID cannot be empty");
        }
        
        if (!isValidOrderId(value)) {
            throw new IllegalArgumentException("Invalid Order ID format: " + value);
        }
        
        this.value = value;
    }
    
    /**
     * Validate the order ID format according to business rules.
     * 
     * @param orderId the order ID to validate
     * @return true if the order ID is valid, false otherwise
     */
    private boolean isValidOrderId(String orderId) {
        // Implement validation rules for order IDs
        // Example: order ID should be alphanumeric and may have specific prefixes
        return orderId.matches("^[A-Za-z0-9\\-_]+$") && orderId.length() >= 5 && orderId.length() <= 50;
    }
    
    /**
     * Get the string value of this order ID.
     * 
     * @return the order ID string value
     */
    public String value() {
        return value;
    }
    
    /**
     * Legacy getter for backward compatibility.
     * 
     * @return the order ID string value
     */
    public String getValue() {
        return value;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DomainValueOrderId that = (DomainValueOrderId) o;
        return Objects.equals(value, that.value);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(value);
    }
    
    @Override
    public String toString() {
        return value;
    }
}