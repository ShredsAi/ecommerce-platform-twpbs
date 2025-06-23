package ai.shreds.application.exceptions;

import java.util.UUID;

/**
 * Thrown when a requested payment is not found
 */
public class ApplicationPaymentNotFoundException extends RuntimeException {

    private final UUID paymentId;

    public ApplicationPaymentNotFoundException(UUID paymentId) {
        super("Payment not found with id: " + paymentId);
        this.paymentId = paymentId;
    }

    public UUID getPaymentId() {
        return paymentId;
    }
}