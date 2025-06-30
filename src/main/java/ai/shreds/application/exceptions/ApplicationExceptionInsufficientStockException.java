package ai.shreds.application.exceptions;

import java.math.BigDecimal;

public class ApplicationExceptionInsufficientStockException extends RuntimeException {
    private final BigDecimal requestedQuantity;
    private final BigDecimal availableQuantity;

    public ApplicationExceptionInsufficientStockException(BigDecimal requested, BigDecimal available) {
        super(String.format("Insufficient stock available. Requested: %s, Available: %s", requested, available));
        this.requestedQuantity = requested;
        this.availableQuantity = available;
    }

    public BigDecimal getRequestedQuantity() {
        return requestedQuantity;
    }

    public BigDecimal getAvailableQuantity() {
        return availableQuantity;
    }
}