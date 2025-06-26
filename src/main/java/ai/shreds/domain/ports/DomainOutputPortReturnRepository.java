package ai.shreds.domain.ports;

import ai.shreds.domain.entities.DomainReturnRequestEntity;
import ai.shreds.shared.enums.SharedReturnStatusEnum;

import java.util.List;

/**
 * Domain output port for return repository operations.
 * Defines the contract for return persistence operations.
 * This interface is implemented by the infrastructure layer.
 */
public interface DomainOutputPortReturnRepository {
    
    /**
     * Save a return request entity.
     * 
     * @param returnRequest the return request entity to save
     * @return the saved return request entity
     */
    DomainReturnRequestEntity save(DomainReturnRequestEntity returnRequest);
    
    /**
     * Find a return request by its ID.
     * 
     * @param returnId the ID of the return to find
     * @return the return request entity, or null if not found
     */
    DomainReturnRequestEntity findById(String returnId);
    
    /**
     * Find all return requests for a specific order.
     * 
     * @param orderId the ID of the order
     * @return list of return request entities for the order
     */
    List<DomainReturnRequestEntity> findByOrderId(String orderId);
    
    /**
     * Find all return requests with a specific status.
     * 
     * @param status the return status to filter by
     * @return list of return request entities with the specified status
     */
    List<DomainReturnRequestEntity> findByReturnStatus(SharedReturnStatusEnum status);
    
    /**
     * Find a return request by its RMA number.
     * 
     * @param rmaNumber the RMA number to search for
     * @return the return request entity, or null if not found
     */
    DomainReturnRequestEntity findByRmaNumber(String rmaNumber);
}