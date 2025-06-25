package ai.shreds.domain.value_objects;

import java.util.Objects;

/**
 * Domain value object representing a Customer ID.
 * Encapsulates validation and business rules related to customer identifiers.
 */
public final class DomainValueCustomerId {
    
    private final String value;
    
    /**
     * Create a new Customer ID value object.
     *
     * @param value the customer ID string value
     * @throws IllegalArgumentException if the customer ID is invalid
     */
    public DomainValueCustomerId(String value) {
        Objects.requireNonNull(value, "Customer ID cannot be null");
        
        if (value.trim().isEmpty()) {
            throw new IllegalArgumentException("Customer ID cannot be empty");
        }
        
        if (!isValidCustomerId(value)) {
            throw new IllegalArgumentException("Invalid Customer ID format: " + value);
        }
        
        this.value = value;
    }
    
    /**
     * Validate the customer ID format according to business rules.
     * 
     * @param customerId the customer ID to validate
     * @return true if the customer ID is valid, false otherwise
     */
    private boolean isValidCustomerId(String customerId) {
        // Implement validation rules for customer IDs
        // Example: customer ID should be alphanumeric and may have specific format requirements
        return customerId.matches("^[A-Za-z0-9\\-_]+$") && customerId.length() >= 5 && customerId.length() <= 50;
    }
    
    /**
     * Get the string value of this customer ID.
     * 
     * @return the customer ID string value
     */
    public String value() {
        return value;
    }
    
    /**
     * Legacy getter for backward compatibility.
     * 
     * @return the customer ID string value
     */
    public String getValue() {
        return value;
    }
    
    /**
     * Check if this customer ID belongs to a guest customer (not registered).
     * 
     * @return true if this is a guest customer ID, false otherwise
     */
    public boolean isGuestCustomer() {
        return value.startsWith("GUEST-");
    }
    
    /**
     * Check if this customer ID represents a business account.
     * 
     * @return true if this is a business customer ID, false otherwise
     */
    public boolean isBusinessCustomer() {
        return value.startsWith("B-") || value.contains("-BUSINESS-");
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DomainValueCustomerId that = (DomainValueCustomerId) o;
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