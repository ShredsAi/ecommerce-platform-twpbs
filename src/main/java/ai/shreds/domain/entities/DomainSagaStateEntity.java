package ai.shreds.domain.entities;

import ai.shreds.domain.value_objects.DomainOrderIdValue;
import ai.shreds.shared.enums.SharedSagaStepEnum;
import ai.shreds.shared.enums.SharedSagaStatusEnum;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * Domain entity representing the state of a saga orchestration.
 */
public class DomainSagaStateEntity {
    private final UUID sagaId;
    private final DomainOrderIdValue orderId;
    private SharedSagaStepEnum currentStep;
    private SharedSagaStatusEnum status;
    private int retryCount;
    private Instant lastActivity;
    private Duration timeoutThreshold;
    private Instant nextRetry;
    private final Instant createdAt;
    private Instant updatedAt;
    private int version;

    /**
     * All-args constructor made public for MapStruct instantiation.
     */
    public DomainSagaStateEntity(UUID sagaId,
                                DomainOrderIdValue orderId,
                                SharedSagaStepEnum currentStep,
                                SharedSagaStatusEnum status,
                                int retryCount,
                                Instant lastActivity,
                                Duration timeoutThreshold,
                                Instant nextRetry,
                                Instant createdAt,
                                Instant updatedAt,
                                int version) {
        if (orderId == null) {
            throw new IllegalArgumentException("Order ID cannot be null");
        }
        if (currentStep == null) {
            throw new IllegalArgumentException("Current step cannot be null");
        }
        if (status == null) {
            throw new IllegalArgumentException("Status cannot be null");
        }
        
        this.sagaId = sagaId != null ? sagaId : UUID.randomUUID();
        this.orderId = orderId;
        this.currentStep = currentStep;
        this.status = status;
        this.retryCount = Math.max(0, retryCount);
        this.lastActivity = lastActivity != null ? lastActivity : Instant.now();
        this.timeoutThreshold = timeoutThreshold != null ? timeoutThreshold : Duration.ofMinutes(30);
        this.nextRetry = nextRetry;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
        this.updatedAt = updatedAt != null ? updatedAt : Instant.now();
        this.version = version;
    }

    /**
     * Creates a new saga state for an order.
     */
    public static DomainSagaStateEntity createNew(DomainOrderIdValue orderId) {
        return new DomainSagaStateEntity(
            UUID.randomUUID(),
            orderId,
            SharedSagaStepEnum.PAYMENT_AUTHORIZATION,
            SharedSagaStatusEnum.IN_PROGRESS,
            0,
            Instant.now(),
            Duration.ofMinutes(30),
            null,
            Instant.now(),
            Instant.now(),
            0
        );
    }

    /**
     * Creates a saga state from existing data.
     */
    public static DomainSagaStateEntity fromData(UUID sagaId,
                                                DomainOrderIdValue orderId,
                                                String currentStep,
                                                String status,
                                                int retryCount,
                                                Instant lastActivity,
                                                Duration timeoutThreshold) {
        return new DomainSagaStateEntity(
            sagaId,
            orderId,
            SharedSagaStepEnum.valueOf(currentStep),
            SharedSagaStatusEnum.valueOf(status),
            retryCount,
            lastActivity,
            timeoutThreshold,
            null,
            lastActivity,
            Instant.now(),
            0
        );
    }

    public UUID getSagaId() {
        return sagaId;
    }

    public DomainOrderIdValue getOrderId() {
        return orderId;
    }

    public SharedSagaStepEnum getCurrentStep() {
        return currentStep;
    }

    public SharedSagaStatusEnum getStatus() {
        return status;
    }

    public int getRetryCount() {
        return retryCount;
    }

    public Instant getLastActivity() {
        return lastActivity;
    }

    public Duration getTimeoutThreshold() {
        return timeoutThreshold;
    }

    public Instant getNextRetry() {
        return nextRetry;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public int getVersion() {
        return version;
    }

    /**
     * Increments the retry count and updates activity timestamp.
     */
    public void incrementRetryCount() {
        this.retryCount++;
        this.lastActivity = Instant.now();
        this.updatedAt = Instant.now();
        
        // Calculate next retry time with exponential backoff
        long backoffSeconds = (long) Math.pow(2, Math.min(retryCount, 8)); // Max 256 seconds
        this.nextRetry = Instant.now().plusSeconds(backoffSeconds);
    }

    /**
     * Updates the current step of the saga.
     */
    public void updateStep(SharedSagaStepEnum step) {
        if (step == null) {
            throw new IllegalArgumentException("Step cannot be null");
        }
        
        this.currentStep = step;
        this.lastActivity = Instant.now();
        this.updatedAt = Instant.now();
        // Reset retry count when moving to new step
        this.retryCount = 0;
        this.nextRetry = null;
    }

    /**
     * Updates the saga status.
     */
    public void updateStatus(SharedSagaStatusEnum newStatus) {
        if (newStatus == null) {
            throw new IllegalArgumentException("Status cannot be null");
        }
        
        this.status = newStatus;
        this.lastActivity = Instant.now();
        this.updatedAt = Instant.now();
    }

    /**
     * Checks if the saga has timed out based on the current time.
     */
    public boolean isTimedOut(Instant now) {
        if (now == null) {
            now = Instant.now();
        }
        
        return status == SharedSagaStatusEnum.IN_PROGRESS &&
               lastActivity.plus(timeoutThreshold).isBefore(now);
    }

    /**
     * Checks if the saga can be retried.
     */
    public boolean canRetry() {
        return retryCount < 5 && 
               status == SharedSagaStatusEnum.IN_PROGRESS;
    }

    /**
     * Marks the saga as completed.
     */
    public void markCompleted() {
        this.status = SharedSagaStatusEnum.COMPLETED;
        this.lastActivity = Instant.now();
        this.updatedAt = Instant.now();
    }

    /**
     * Marks the saga as failed.
     */
    public void markFailed() {
        this.status = SharedSagaStatusEnum.FAILED;
        this.lastActivity = Instant.now();
        this.updatedAt = Instant.now();
    }

    /**
     * Marks the saga as compensating.
     */
    public void markCompensating() {
        this.status = SharedSagaStatusEnum.COMPENSATING;
        this.lastActivity = Instant.now();
        this.updatedAt = Instant.now();
    }

    /**
     * Marks the saga as timed out.
     */
    public void markTimedOut() {
        this.status = SharedSagaStatusEnum.TIMED_OUT;
        this.lastActivity = Instant.now();
        this.updatedAt = Instant.now();
    }
}