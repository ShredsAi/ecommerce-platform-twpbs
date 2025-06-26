package ai.shreds.domain.exceptions;

import java.util.UUID;

/**
 * Exception thrown when a requested order cannot be found.
 */
public class DomainOrderNotFoundException extends RuntimeException {
    private final UUID orderId;

    public DomainOrderNotFoundException(UUID orderId) {
        super(String.format("Order not found for id: %s", orderId));
        this.orderId = orderId;
    }
    
    public DomainOrderNotFoundException(UUID orderId, String message) {
        super(String.format("Order not found for id: %s - %s", orderId, message));
        this.orderId = orderId;
    }
    
    public DomainOrderNotFoundException(UUID orderId, Throwable cause) {
        super(String.format("Order not found for id: %s", orderId), cause);
        this.orderId = orderId;
    }

    public UUID getOrderId() {
        return orderId;
    }
}