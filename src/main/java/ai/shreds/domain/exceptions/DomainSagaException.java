package ai.shreds.domain.exceptions;

import java.util.UUID;

/**
 * Exception thrown when saga processing encounters an error.
 */
public class DomainSagaException extends RuntimeException {
    private final UUID sagaId;
    private final UUID orderId;

    public DomainSagaException(String message, Throwable cause) {
        super(message, cause);
        this.sagaId = null;
        this.orderId = null;
    }

    public DomainSagaException(String message, UUID sagaId, UUID orderId) {
        super(message);
        this.sagaId = sagaId;
        this.orderId = orderId;
    }

    public DomainSagaException(String message, UUID sagaId, UUID orderId, Throwable cause) {
        super(message, cause);
        this.sagaId = sagaId;
        this.orderId = orderId;
    }

    public UUID getSagaId() {
        return sagaId;
    }

    public UUID getOrderId() {
        return orderId;
    }
}