package ai.shreds.domain.value_objects;

import java.time.Instant;
import java.util.Objects;

/**
 * Domain value object representing an RMA (Return Merchandise Authorization) number.
 * Contains business logic for generating and validating RMA numbers.
 */
public class DomainRmaNumberValue {
    
    private final String value;
    private static final String DEFAULT_PREFIX = "RMA-";
    
    /**
     * Create a new RMA number with a given prefix.
     */
    public DomainRmaNumberValue(String prefix) {
        this.value = generateUnique(prefix);
    }
    
    /**
     * Create a new RMA number with the default prefix.
     */
    public DomainRmaNumberValue() {
        this(DEFAULT_PREFIX);
    }
    
    /**
     * Create an RMA number from an existing value.
     */
    public DomainRmaNumberValue(String prefix, String existingRmaNumber) {
        if (!existingRmaNumber.startsWith(prefix)) {
            throw new IllegalArgumentException("RMA number must start with the specified prefix");
        }
        if (!validate(existingRmaNumber)) {
            throw new IllegalArgumentException("Invalid RMA number format");
        }
        this.value = existingRmaNumber;
    }
    
    /**
     * Generate a unique RMA number.
     * Uses a combination of prefix, timestamp, and random characters for uniqueness.
     */
    public String generateUnique(String prefix) {
        // Use base-36 encoding of current timestamp plus a random component
        // This provides a relatively short but unique identifier
        String timestamp = Long.toString(Instant.now().toEpochMilli(), 36).toUpperCase();
        String random = Long.toString(Math.abs(java.util.UUID.randomUUID().getMostSignificantBits()), 36)
                           .substring(0, 4)
                           .toUpperCase();
        
        return prefix + timestamp + "-" + random;
    }
    
    /**
     * Validate an RMA number format.
     */
    public boolean validate() {
        return validate(this.value);
    }
    
    /**
     * Validate that an RMA number follows the required format.
     */
    public boolean validate(String rmaNumber) {
        if (rmaNumber == null || rmaNumber.length() < 8) {
            return false;
        }
        
        // Check that it starts with a valid prefix
        if (!rmaNumber.startsWith(DEFAULT_PREFIX) && 
            !rmaNumber.matches("^[A-Z]{3,5}-.*")) {
            return false;
        }
        
        // Check that it contains a hyphen after the timestamp component
        int hyphenPos = rmaNumber.indexOf('-', DEFAULT_PREFIX.length());
        if (hyphenPos < 0 || hyphenPos == rmaNumber.length() - 1) {
            return false;
        }
        
        // Check that the random part exists and has reasonable length
        String randomPart = rmaNumber.substring(hyphenPos + 1);
        return randomPart.length() >= 2 && randomPart.matches("^[A-Z0-9]+$");
    }
    
    /**
     * Get the RMA number value.
     */
    public String value() {
        return this.value;
    }
    
    @Override
    public String toString() {
        return this.value;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DomainRmaNumberValue that = (DomainRmaNumberValue) o;
        return Objects.equals(value, that.value);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(value);
    }
}