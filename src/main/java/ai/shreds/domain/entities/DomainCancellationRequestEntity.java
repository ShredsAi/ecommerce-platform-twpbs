package ai.shreds.domain.entities;

import ai.shreds.domain.value_objects.DomainOrderIdValue;
import java.time.Instant;
import java.util.UUID;

/**
 * Domain entity representing a cancellation request for an order.
 */
public class DomainCancellationRequestEntity {
    private final UUID cancellationId;
    private final DomainOrderIdValue orderId;
    private final String reason;
    private final boolean refundRequired;
    private String status;
    private final Instant requestedAt;
    private Instant processedAt;

    private DomainCancellationRequestEntity(UUID cancellationId,
                                           DomainOrderIdValue orderId,
                                           String reason,
                                           boolean refundRequired,
                                           String status,
                                           Instant requestedAt,
                                           Instant processedAt) {
        if (orderId == null) {
            throw new IllegalArgumentException("Order ID cannot be null");
        }
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("Cancellation reason cannot be null or blank");
        }
        
        this.cancellationId = cancellationId != null ? cancellationId : UUID.randomUUID();
        this.orderId = orderId;
        this.reason = reason;
        this.refundRequired = refundRequired;
        this.status = status != null ? status : "PENDING";
        this.requestedAt = requestedAt != null ? requestedAt : Instant.now();
        this.processedAt = processedAt;
    }

    /**
     * Creates a new cancellation request.
     */
    public static DomainCancellationRequestEntity create(DomainOrderIdValue orderId,
                                                        String reason,
                                                        boolean refundRequired) {
        return new DomainCancellationRequestEntity(
            UUID.randomUUID(),
            orderId,
            reason,
            refundRequired,
            "PENDING",
            Instant.now(),
            null
        );
    }

    /**
     * Creates a cancellation request from shared data.
     */
    public static DomainCancellationRequestEntity fromSharedData(UUID orderId,
                                                                String reason,
                                                                boolean refundRequired,
                                                                Instant timestamp) {
        DomainOrderIdValue orderIdValue = new DomainOrderIdValue(orderId);
        return new DomainCancellationRequestEntity(
            UUID.randomUUID(),
            orderIdValue,
            reason,
            refundRequired,
            "PENDING",
            timestamp,
            null
        );
    }

    public UUID getCancellationId() {
        return cancellationId;
    }

    public DomainOrderIdValue getOrderId() {
        return orderId;
    }

    public String getReason() {
        return reason;
    }

    public boolean isRefundRequired() {
        return refundRequired;
    }

    public String getStatus() {
        return status;
    }

    public Instant getRequestedAt() {
        return requestedAt;
    }

    public Instant getProcessedAt() {
        return processedAt;
    }

    /**
     * Marks the cancellation as approved.
     */
    public void approve() {
        this.status = "APPROVED";
        this.processedAt = Instant.now();
    }

    /**
     * Marks the cancellation as rejected with reason.
     */
    public void reject(String rejectionReason) {
        this.status = "REJECTED";
        this.processedAt = Instant.now();
    }

    /**
     * Marks the cancellation as completed.
     */
    public void complete() {
        this.status = "COMPLETED";
        this.processedAt = Instant.now();
    }

    /**
     * Checks if the cancellation is in a final state.
     */
    public boolean isInFinalState() {
        return "COMPLETED".equals(status) || "REJECTED".equals(status);
    }

    /**
     * Checks if the cancellation can be processed.
     */
    public boolean canBeProcessed() {
        return "PENDING".equals(status) || "APPROVED".equals(status);
    }
}