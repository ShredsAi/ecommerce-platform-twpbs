package ai.shreds.domain.exceptions;

import ai.shreds.domain.value_objects.DomainPaymentStatusEnum;

/**
 * Exception thrown when an invalid payment state transition is attempted.
 */
public class DomainInvalidStateException extends RuntimeException {
    private final DomainPaymentStatusEnum currentState;
    private final DomainPaymentStatusEnum attemptedState;

    public DomainInvalidStateException(
            DomainPaymentStatusEnum currentState,
            DomainPaymentStatusEnum attemptedState) {
        super(String.format("Invalid state transition: cannot transition from %s to %s",
                currentState, attemptedState));
        this.currentState = currentState;
        this.attemptedState = attemptedState;
    }

    public DomainPaymentStatusEnum getCurrentState() {
        return currentState;
    }

    public DomainPaymentStatusEnum getAttemptedState() {
        return attemptedState;
    }
}