package ai.shreds.domain.ports;

import ai.shreds.domain.entities.DomainCancellationRequestEntity;
import ai.shreds.shared.enums.SharedCancellationStatusEnum;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Domain output port for cancellation repository operations.
 * Defines the contract for cancellation persistence operations.
 * This interface is implemented by the infrastructure layer.
 */
public interface DomainOutputPortCancellationRepository {
    
    /**
     * Save a cancellation request entity.
     * 
     * @param cancellation the cancellation request entity to save
     * @return the saved cancellation request entity
     */
    DomainCancellationRequestEntity save(DomainCancellationRequestEntity cancellation);
    
    /**
     * Find a cancellation request by its ID.
     * 
     * @param cancellationId the ID of the cancellation to find
     * @return the cancellation request entity, or null if not found
     */
    DomainCancellationRequestEntity findById(String cancellationId);
    
    /**
     * Find all cancellation requests for a specific order.
     * 
     * @param orderId the ID of the order
     * @return list of cancellation request entities for the order
     */
    List<DomainCancellationRequestEntity> findByOrderId(String orderId);
    
    /**
     * Find all cancellation requests with a specific status.
     * 
     * @param status the cancellation status to filter by
     * @return list of cancellation request entities with the specified status
     */
    List<DomainCancellationRequestEntity> findByCancellationStatus(SharedCancellationStatusEnum status);
    
    /**
     * Find all pending cancellation requests that were created before a specific cutoff time.
     * This is used for timeout processing.
     * 
     * @param cutoff the cutoff time
     * @return list of pending cancellation requests older than the cutoff
     */
    List<DomainCancellationRequestEntity> findPendingBefore(LocalDateTime cutoff);
}