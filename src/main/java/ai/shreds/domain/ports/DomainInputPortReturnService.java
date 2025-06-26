package ai.shreds.domain.ports;

import ai.shreds.domain.entities.DomainReturnRequestEntity;
import ai.shreds.domain.entities.DomainReturnItemEntity;
import ai.shreds.shared.enums.SharedReturnReasonEnum;
import ai.shreds.shared.enums.SharedReturnStatusEnum;
import ai.shreds.shared.dtos.SharedOrderSnapshotDTO;

import java.util.List;

/**
 * Domain input port for return service operations.
 * Defines the contract for return-related business operations.
 */
public interface DomainInputPortReturnService {
    
    /**
     * Request a new return for an order.
     * 
     * @param orderSnapshot the order snapshot containing order details
     * @param items the list of items to be returned
     * @param reason the reason for return
     * @return the created return request entity
     * @throws IllegalArgumentException if the request is invalid
     * @throws IllegalStateException if return is not allowed
     */
    DomainReturnRequestEntity requestReturn(
        SharedOrderSnapshotDTO orderSnapshot, 
        List<DomainReturnItemEntity> items, 
        SharedReturnReasonEnum reason
    );
    
    /**
     * Update the status of a return request.
     * 
     * @param returnId the ID of the return to update
     * @param newStatus the new status to set
     * @return the updated return request entity
     * @throws IllegalArgumentException if the return ID is invalid
     * @throws IllegalStateException if status transition is not allowed
     */
    DomainReturnRequestEntity updateReturnStatus(String returnId, SharedReturnStatusEnum newStatus);
    
    /**
     * Process a return when it's received at the warehouse.
     * 
     * @param returnId the ID of the return to process
     * @return the updated return request entity
     * @throws IllegalArgumentException if the return ID is invalid
     * @throws IllegalStateException if return cannot be processed
     */
    DomainReturnRequestEntity processReturnReceived(String returnId);
    
    /**
     * Complete the refund for a return request.
     * 
     * @param returnId the ID of the return to complete refund for
     * @return the updated return request entity
     * @throws IllegalArgumentException if the return ID is invalid
     * @throws IllegalStateException if refund cannot be completed
     */
    DomainReturnRequestEntity completeRefund(String returnId);
    
    /**
     * Find a return request by its ID.
     * 
     * @param returnId the ID of the return to find
     * @return the return request entity, or null if not found
     */
    DomainReturnRequestEntity findReturn(String returnId);
    
    /**
     * Find all return requests for a specific order.
     * 
     * @param orderId the ID of the order
     * @return list of return request entities for the order
     */
    List<DomainReturnRequestEntity> findReturnsByOrder(String orderId);
}