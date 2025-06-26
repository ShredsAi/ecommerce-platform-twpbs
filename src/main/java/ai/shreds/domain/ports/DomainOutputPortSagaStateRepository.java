package ai.shreds.domain.ports;

import ai.shreds.domain.entities.DomainSagaStateEntity;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Domain output port for saga state repository operations.
 * This port defines the contract for saga state persistence operations.
 */
public interface DomainOutputPortSagaStateRepository {

    /**
     * Saves a saga state entity.
     * @param sagaState The saga state entity to save
     * @return The saved saga state entity
     * @throws ai.shreds.domain.exceptions.DomainSagaException if save operation fails
     */
    DomainSagaStateEntity save(DomainSagaStateEntity sagaState);

    /**
     * Finds a saga state by order ID.
     * @param orderId The order ID to search for
     * @return Optional containing the saga state if found, empty otherwise
     */
    Optional<DomainSagaStateEntity> findByOrderId(UUID orderId);

    /**
     * Finds a saga state by saga ID.
     * @param sagaId The saga ID to search for
     * @return Optional containing the saga state if found, empty otherwise
     */
    Optional<DomainSagaStateEntity> findBySagaId(UUID sagaId);

    /**
     * Finds all saga states that have timed out.
     * @param cutoff The cutoff time for timeout detection
     * @return List of timed-out saga states
     */
    List<DomainSagaStateEntity> findTimedOut(LocalDateTime cutoff);

    /**
     * Finds all saga states with specific status.
     * @param status The saga status to filter by
     * @return List of saga states with the given status
     */
    List<DomainSagaStateEntity> findByStatus(String status);

    /**
     * Deletes a saga state by saga ID.
     * @param sagaId The saga ID to delete
     */
    void deleteBySagaId(UUID sagaId);

    /**
     * Checks if a saga state exists for the given order ID.
     * @param orderId The order ID to check
     * @return true if saga state exists, false otherwise
     */
    boolean existsByOrderId(UUID orderId);
}