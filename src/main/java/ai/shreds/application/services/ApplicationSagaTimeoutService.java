package ai.shreds.application.services;

import ai.shreds.application.exceptions.ApplicationTimeoutException;
import ai.shreds.domain.entities.DomainSagaStateEntity;
import ai.shreds.domain.ports.DomainOutputPortOrderRepository;
import ai.shreds.domain.ports.DomainOutputPortSagaStateRepository;
import ai.shreds.shared.dtos.SharedTimeoutDetailDTO;
import ai.shreds.shared.enums.SharedSagaStatusEnum;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Service for detecting and processing saga timeouts.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ApplicationSagaTimeoutService {

    private final DomainOutputPortSagaStateRepository sagaStateRepository;
    private final DomainOutputPortOrderRepository orderRepository;

    private static final int MAX_RETRY_COUNT = 5;
    private static final long TIMEOUT_THRESHOLD_MINUTES = 30;

    /**
     * Find all saga states that have timed out based on the cutoff timestamp.
     */
    public List<DomainSagaStateEntity> detectTimeouts(LocalDateTime cutoffTime) {
        try {
            log.debug("Detecting timeouts with cutoff time: {}", cutoffTime);
            List<DomainSagaStateEntity> timedOutSagas = sagaStateRepository.findTimedOut(cutoffTime);
            log.info("Found {} timed out sagas", timedOutSagas.size());
            return timedOutSagas;
        } catch (Exception e) {
            log.error("Error detecting timeouts", e);
            throw new ApplicationTimeoutException("Failed to detect timeouts: " + e.getMessage());
        }
    }

    /**
     * Process a single saga state timeout and produce a detail DTO.
     */
    public SharedTimeoutDetailDTO processTimeout(DomainSagaStateEntity sagaState) {
        try {
            log.debug("Processing timeout for saga: {}, retry count: {}", 
                sagaState.getSagaId(), sagaState.getRetryCount());

            boolean shouldEscalate = shouldEscalate(sagaState.getRetryCount());
            
            if (shouldEscalate) {
                // Mark saga as timed out if max retries exceeded
                sagaState.setStatus(SharedSagaStatusEnum.TIMED_OUT);
                sagaState.setLastActivity(Instant.now());
                sagaStateRepository.save(sagaState);
                
                log.warn("Saga {} escalated due to timeout after {} retries", 
                    sagaState.getSagaId(), sagaState.getRetryCount());
                
                return new SharedTimeoutDetailDTO(
                    sagaState.getSagaId(),
                    sagaState.getOrderId(),
                    sagaState.getCurrentStep().name(),
                    "ESCALATED",
                    true,
                    "Maximum retries exceeded, saga marked as timed out"
                );
            } else {
                // Increment retry count and schedule next retry
                sagaState.incrementRetryCount();
                LocalDateTime nextRetry = calculateNextRetryTime(sagaState.getRetryCount());
                sagaState.setNextRetry(nextRetry);
                sagaState.setLastActivity(Instant.now());
                sagaStateRepository.save(sagaState);
                
                log.info("Saga {} scheduled for retry #{} at {}", 
                    sagaState.getSagaId(), sagaState.getRetryCount(), nextRetry);
                
                return new SharedTimeoutDetailDTO(
                    sagaState.getSagaId(),
                    sagaState.getOrderId(),
                    sagaState.getCurrentStep().name(),
                    "RETRIED",
                    true,
                    "Retry scheduled for " + nextRetry
                );
            }
        } catch (Exception ex) {
            log.error("Error processing timeout for saga: {}", sagaState.getSagaId(), ex);
            throw new ApplicationTimeoutException(
                sagaState.getOrderId(), 
                sagaState.getRetryCount(), 
                "Error processing timeout for saga: " + sagaState.getSagaId()
            );
        }
    }

    /**
     * Calculate next retry time using exponential backoff.
     * Formula: base_delay * (2^retry_count) minutes
     */
    public LocalDateTime calculateNextRetryTime(int retryCount) {
        try {
            // Base delay of 1 minute, exponentially increasing
            long delayMinutes = (long) Math.pow(2, Math.min(retryCount, 6)); // Cap at 64 minutes max delay
            LocalDateTime nextRetry = LocalDateTime.now().plusMinutes(delayMinutes);
            
            log.debug("Calculated next retry time: {} (delay: {} minutes)", nextRetry, delayMinutes);
            return nextRetry;
        } catch (Exception e) {
            log.warn("Error calculating retry time, using default 5 minutes", e);
            return LocalDateTime.now().plusMinutes(5);
        }
    }

    /**
     * Determine if the saga should escalate after a number of retries.
     */
    public boolean shouldEscalate(int retryCount) {
        boolean escalate = retryCount >= MAX_RETRY_COUNT;
        log.debug("Should escalate for retry count {}: {}", retryCount, escalate);
        return escalate;
    }

    /**
     * Check if a saga has been inactive for too long based on current time.
     */
    public boolean isTimedOut(DomainSagaStateEntity sagaState) {
        try {
            LocalDateTime cutoffTime = LocalDateTime.now().minusMinutes(TIMEOUT_THRESHOLD_MINUTES);
            boolean timedOut = sagaState.getLastActivity().isBefore(cutoffTime.atZone(java.time.ZoneId.systemDefault()).toInstant());
            
            if (timedOut) {
                log.debug("Saga {} is timed out. Last activity: {}, cutoff: {}", 
                    sagaState.getSagaId(), sagaState.getLastActivity(), cutoffTime);
            }
            
            return timedOut;
        } catch (Exception e) {
            log.error("Error checking if saga is timed out: {}", sagaState.getSagaId(), e);
            return false;
        }
    }

    /**
     * Get the timeout threshold in minutes for saga processing.
     */
    public long getTimeoutThresholdMinutes() {
        return TIMEOUT_THRESHOLD_MINUTES;
    }

    /**
     * Get the maximum retry count before escalation.
     */
    public int getMaxRetryCount() {
        return MAX_RETRY_COUNT;
    }

    /**
     * Reset retry count for a saga (used in manual intervention scenarios).
     */
    @Transactional
    public void resetRetryCount(DomainSagaStateEntity sagaState) {
        try {
            sagaState.setRetryCount(0);
            sagaState.setLastActivity(Instant.now());
            sagaState.setNextRetry(null);
            sagaStateRepository.save(sagaState);
            
            log.info("Reset retry count for saga: {}", sagaState.getSagaId());
        } catch (Exception e) {
            log.error("Error resetting retry count for saga: {}", sagaState.getSagaId(), e);
            throw new ApplicationTimeoutException(
                sagaState.getOrderId(),
                sagaState.getRetryCount(),
                "Failed to reset retry count: " + e.getMessage()
            );
        }
    }

    /**
     * Force escalation of a saga (used in manual intervention scenarios).
     */
    @Transactional
    public void forceEscalation(DomainSagaStateEntity sagaState, String reason) {
        try {
            sagaState.setStatus(SharedSagaStatusEnum.TIMED_OUT);
            sagaState.setLastActivity(Instant.now());
            sagaStateRepository.save(sagaState);
            
            log.warn("Forced escalation for saga: {} - Reason: {}", sagaState.getSagaId(), reason);
        } catch (Exception e) {
            log.error("Error forcing escalation for saga: {}", sagaState.getSagaId(), e);
            throw new ApplicationTimeoutException(
                sagaState.getOrderId(),
                sagaState.getRetryCount(),
                "Failed to force escalation: " + e.getMessage()
            );
        }
    }
}