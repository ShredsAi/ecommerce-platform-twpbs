package ai.shreds.domain.commands;

import ai.shreds.domain.value_objects.DomainPaymentIdValue;
import ai.shreds.domain.value_objects.DomainPaymentStatusEnum;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Command to update payment status based on external webhook or reconciliation.
 * This command is typically used when payment status changes are received from external processors.
 */
public class DomainUpdateStatusCommand {
    private final DomainPaymentIdValue paymentId;
    private final DomainPaymentStatusEnum newStatus;
    private final Map<String, Object> processorResponse;

    public DomainUpdateStatusCommand(
            DomainPaymentIdValue paymentId,
            DomainPaymentStatusEnum newStatus,
            Map<String, Object> processorResponse) {
        this.paymentId = Objects.requireNonNull(paymentId, "paymentId cannot be null");
        this.newStatus = Objects.requireNonNull(newStatus, "newStatus cannot be null");
        this.processorResponse = processorResponse != null ? 
            new HashMap<>(processorResponse) : new HashMap<>();
        
        validateCommand();
    }

    private void validateCommand() {
        // Validate that the new status is a terminal state for payments
        if (newStatus != DomainPaymentStatusEnum.SUCCEEDED && 
            newStatus != DomainPaymentStatusEnum.FAILED) {
            throw new IllegalArgumentException(
                "Payment status updates must be to terminal states (SUCCEEDED or FAILED), got: " + newStatus
            );
        }
    }

    public DomainPaymentIdValue getPaymentId() {
        return paymentId;
    }

    public DomainPaymentStatusEnum getNewStatus() {
        return newStatus;
    }

    public Map<String, Object> getProcessorResponse() {
        return new HashMap<>(processorResponse);
    }

    /**
     * Checks if this command represents a successful payment update.
     */
    public boolean isSuccessfulUpdate() {
        return newStatus == DomainPaymentStatusEnum.SUCCEEDED;
    }

    /**
     * Checks if this command represents a failed payment update.
     */
    public boolean isFailedUpdate() {
        return newStatus == DomainPaymentStatusEnum.FAILED;
    }

    /**
     * Gets a specific value from the processor response.
     */
    public Object getProcessorResponseValue(String key) {
        return processorResponse.get(key);
    }

    /**
     * Gets the processor transaction ID if available.
     */
    public String getProcessorTransactionId() {
        Object transactionId = processorResponse.get("transaction_id");
        return transactionId != null ? transactionId.toString() : null;
    }

    /**
     * Gets the failure reason if this is a failed update.
     */
    public String getFailureReason() {
        if (!isFailedUpdate()) {
            return null;
        }
        Object reason = processorResponse.get("failure_reason");
        return reason != null ? reason.toString() : "Unknown failure";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DomainUpdateStatusCommand)) return false;
        DomainUpdateStatusCommand that = (DomainUpdateStatusCommand) o;
        return paymentId.equals(that.paymentId) && 
               newStatus == that.newStatus && 
               processorResponse.equals(that.processorResponse);
    }

    @Override
    public int hashCode() {
        return Objects.hash(paymentId, newStatus, processorResponse);
    }

    @Override
    public String toString() {
        return "DomainUpdateStatusCommand{" +
                "paymentId=" + paymentId +
                ", newStatus=" + newStatus +
                ", processorResponse=" + processorResponse +
                '}';
    }
}