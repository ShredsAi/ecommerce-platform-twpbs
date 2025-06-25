package ai.shreds.domain.value_objects;

import java.util.Objects;

/**
 * Domain value object representing a Product ID.
 * Encapsulates validation and business rules related to product identifiers.
 */
public final class DomainValueProductId {
    
    private final String value;
    
    /**
     * Create a new Product ID value object.
     *
     * @param value the product ID string value
     * @throws IllegalArgumentException if the product ID is invalid
     */
    public DomainValueProductId(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("ProductId cannot be null or empty");
        }
        
        if (!isValidProductId(value)) {
            throw new IllegalArgumentException("Invalid Product ID format: " + value);
        }
        
        this.value = value;
    }
    
    /**
     * Validate the product ID format according to business rules.
     * 
     * @param productId the product ID to validate
     * @return true if the product ID is valid, false otherwise
     */
    private boolean isValidProductId(String productId) {
        // Implement validation rules for product IDs
        // Example: product ID should be alphanumeric and may have specific format requirements
        return productId.matches("^[A-Za-z0-9\\-_]+$") && productId.length() >= 3 && productId.length() <= 50;
    }
    
    /**
     * Get the string value of this product ID.
     * 
     * @return the product ID string value
     */
    public String value() {
        return value;
    }
    
    /**
     * Legacy getter for backward compatibility.
     * 
     * @return the product ID string value
     */
    public String getValue() {
        return value;
    }
    
    /**
     * Check if this product ID represents a digital product.
     * 
     * @return true if this is a digital product ID, false otherwise
     */
    public boolean isDigitalProduct() {
        return value.startsWith("DIG-") || value.contains("-DIGITAL-");
    }
    
    /**
     * Check if this product ID represents a subscription-based product.
     * 
     * @return true if this is a subscription product ID, false otherwise
     */
    public boolean isSubscriptionProduct() {
        return value.startsWith("SUB-") || value.contains("-SUBSCRIPTION-");
    }
    
    /**
     * Check if this product ID represents a bundle product.
     * 
     * @return true if this is a bundle product ID, false otherwise
     */
    public boolean isBundleProduct() {
        return value.startsWith("BUNDLE-") || value.contains("-BUNDLE-");
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DomainValueProductId)) return false;
        DomainValueProductId that = (DomainValueProductId) o;
        return value.equals(that.value);
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