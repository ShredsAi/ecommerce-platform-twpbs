package ai.shreds.domain.ports;

import ai.shreds.domain.entities.DomainCancellationRequestEntity;
import ai.shreds.shared.enums.SharedCancellationReasonEnum;
import ai.shreds.shared.dtos.SharedOrderSnapshotDTO;

import java.util.List;

/**
 * Domain input port for cancellation service operations.
 * Defines the contract for cancellation-related business operations.
 */
public interface DomainInputPortCancellationService {
    
    /**
     * Request a new cancellation for an order.
     * 
     * @param orderSnapshot the order snapshot containing order details
     * @param reason the reason for cancellation
     * @param notes optional notes about the cancellation
     * @return the created cancellation request entity
     * @throws IllegalArgumentException if the request is invalid
     * @throws IllegalStateException if cancellation is not allowed
     */
    DomainCancellationRequestEntity requestCancellation(
        SharedOrderSnapshotDTO orderSnapshot, 
        SharedCancellationReasonEnum reason, 
        String notes
    );
    
    /**
     * Approve a pending cancellation request.
     * 
     * @param cancellationId the ID of the cancellation to approve
     * @return the updated cancellation request entity
     * @throws IllegalArgumentException if the cancellation ID is invalid
     * @throws IllegalStateException if cancellation cannot be approved
     */
    DomainCancellationRequestEntity approveCancellation(String cancellationId);
    
    /**
     * Reject a pending cancellation request.
     * 
     * @param cancellationId the ID of the cancellation to reject
     * @param reason the reason for rejection
     * @return the updated cancellation request entity
     * @throws IllegalArgumentException if the cancellation ID is invalid
     * @throws IllegalStateException if cancellation cannot be rejected
     */
    DomainCancellationRequestEntity rejectCancellation(String cancellationId, String reason);
    
    /**
     * Complete a cancellation request after all coordination tasks are finished.
     * 
     * @param cancellationId the ID of the cancellation to complete
     * @return the updated cancellation request entity
     * @throws IllegalArgumentException if the cancellation ID is invalid
     * @throws IllegalStateException if cancellation cannot be completed
     */
    DomainCancellationRequestEntity completeCancellation(String cancellationId);
    
    /**
     * Find a cancellation request by its ID.
     * 
     * @param cancellationId the ID of the cancellation to find
     * @return the cancellation request entity, or null if not found
     */
    DomainCancellationRequestEntity findCancellation(String cancellationId);
    
    /**
     * Find all cancellation requests for a specific order.
     * 
     * @param orderId the ID of the order
     * @return list of cancellation request entities for the order
     */
    List<DomainCancellationRequestEntity> findCancellationsByOrder(String orderId);
}