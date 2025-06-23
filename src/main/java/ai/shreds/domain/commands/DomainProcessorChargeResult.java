package ai.shreds.domain.commands;

import ai.shreds.domain.value_objects.DomainPaymentStatusEnum;
import ai.shreds.domain.value_objects.DomainProcessorResponseValue;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Result of a payment processor charge operation.
 * Encapsulates the outcome of charging a payment through an external processor.
 */
public class DomainProcessorChargeResult {
    private final DomainPaymentStatusEnum status;
    private final DomainProcessorResponseValue processorResponse;
    private final boolean requiresAction;
    private final Map<String, Object> nextAction;

    public DomainProcessorChargeResult(
            DomainPaymentStatusEnum status,
            DomainProcessorResponseValue processorResponse,
            boolean requiresAction,
            Map<String, Object> nextAction) {
        this.status = Objects.requireNonNull(status, "status cannot be null");
        this.processorResponse = Objects.requireNonNull(processorResponse, "processorResponse cannot be null");
        this.requiresAction = requiresAction;
        this.nextAction = nextAction != null ? 
            Collections.unmodifiableMap(new HashMap<>(nextAction)) : Collections.emptyMap();
        
        validateResult();
    }

    /**
     * Creates a successful charge result.
     */
    public static DomainProcessorChargeResult success(DomainProcessorResponseValue processorResponse) {
        return new DomainProcessorChargeResult(
            DomainPaymentStatusEnum.SUCCEEDED,
            processorResponse,
            false,
            Collections.emptyMap()
        );
    }

    /**
     * Creates a failed charge result.
     */
    public static DomainProcessorChargeResult failure(DomainProcessorResponseValue processorResponse) {
        return new DomainProcessorChargeResult(
            DomainPaymentStatusEnum.FAILED,
            processorResponse,
            false,
            Collections.emptyMap()
        );
    }

    /**
     * Creates a charge result that requires additional action (e.g., 3D Secure).
     */
    public static DomainProcessorChargeResult requiresAction(
            DomainProcessorResponseValue processorResponse,
            Map<String, Object> nextAction) {
        return new DomainProcessorChargeResult(
            DomainPaymentStatusEnum.PROCESSING,
            processorResponse,
            true,
            nextAction
        );
    }

    /**
     * Creates a processing charge result (async processing).
     */
    public static DomainProcessorChargeResult processing(DomainProcessorResponseValue processorResponse) {
        return new DomainProcessorChargeResult(
            DomainPaymentStatusEnum.PROCESSING,
            processorResponse,
            false,
            Collections.emptyMap()
        );
    }

    private void validateResult() {
        // Validate consistency between requiresAction and nextAction
        if (requiresAction && nextAction.isEmpty()) {
            throw new IllegalArgumentException("nextAction cannot be empty when requiresAction is true");
        }
        
        if (!requiresAction && !nextAction.isEmpty()) {
            throw new IllegalArgumentException("nextAction should be empty when requiresAction is false");
        }
        
        // Validate status consistency
        if (requiresAction && status != DomainPaymentStatusEnum.PROCESSING) {
            throw new IllegalArgumentException(
                "Status must be PROCESSING when requiresAction is true, got: " + status
            );
        }
    }

    public DomainPaymentStatusEnum getStatus() {
        return status;
    }

    public DomainProcessorResponseValue getProcessorResponse() {
        return processorResponse;
    }

    public boolean isRequiresAction() {
        return requiresAction;
    }

    public Map<String, Object> getNextAction() {
        return nextAction;
    }

    /**
     * Checks if the charge was successful.
     */
    public boolean isSuccessful() {
        return status == DomainPaymentStatusEnum.SUCCEEDED;
    }

    /**
     * Checks if the charge failed.
     */
    public boolean isFailed() {
        return status == DomainPaymentStatusEnum.FAILED;
    }

    /**
     * Checks if the charge is still processing.
     */
    public boolean isProcessing() {
        return status == DomainPaymentStatusEnum.PROCESSING;
    }

    /**
     * Gets the processor transaction ID if available.
     */
    public String getProcessorTransactionId() {
        return processorResponse.getProcessorId();
    }

    /**
     * Gets the 3D Secure challenge URL if this result requires 3DS action.
     */
    public String getThreeDSecureChallengeUrl() {
        if (!requiresAction) {
            return null;
        }
        Object challengeUrl = nextAction.get("three_d_secure_challenge_url");
        return challengeUrl != null ? challengeUrl.toString() : null;
    }

    /**
     * Gets the type of action required.
     */
    public String getActionType() {
        if (!requiresAction) {
            return null;
        }
        Object actionType = nextAction.get("action_type");
        return actionType != null ? actionType.toString() : "unknown";
    }

    /**
     * Gets the decline code if the charge was declined.
     */
    public String getDeclineCode() {
        if (!isFailed()) {
            return null;
        }
        return processorResponse.getResponseCode();
    }

    /**
     * Gets the failure reason if the charge failed.
     */
    public String getFailureReason() {
        if (!isFailed()) {
            return null;
        }
        return processorResponse.getResponseMessage();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DomainProcessorChargeResult)) return false;
        DomainProcessorChargeResult that = (DomainProcessorChargeResult) o;
        return requiresAction == that.requiresAction && 
               status == that.status && 
               processorResponse.equals(that.processorResponse) && 
               nextAction.equals(that.nextAction);
    }

    @Override
    public int hashCode() {
        return Objects.hash(status, processorResponse, requiresAction, nextAction);
    }

    @Override
    public String toString() {
        return "DomainProcessorChargeResult{" +
                "status=" + status +
                ", processorResponse=" + processorResponse +
                ", requiresAction=" + requiresAction +
                ", nextAction=" + nextAction +
                '}';
    }
}