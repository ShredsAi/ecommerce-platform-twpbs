package ai.shreds.domain.ports;

import ai.shreds.domain.entities.DomainRefundTransactionEntity;

import java.util.List;

/**
 * Domain output port for refund transaction repository operations.
 * Defines the contract for refund transaction persistence operations.
 * This interface is implemented by the infrastructure layer.
 */
public interface DomainOutputPortRefundTransactionRepository {
    
    /**
     * Save a refund transaction entity.
     * 
     * @param refund the refund transaction entity to save
     * @return the saved refund transaction entity
     */
    DomainRefundTransactionEntity save(DomainRefundTransactionEntity refund);
    
    /**
     * Find a refund transaction by its ID.
     * 
     * @param refundId the ID of the refund to find
     * @return the refund transaction entity, or null if not found
     */
    DomainRefundTransactionEntity findById(String refundId);
    
    /**
     * Find all refund transactions for a specific cancellation.
     * 
     * @param cancellationId the ID of the cancellation
     * @return list of refund transaction entities for the cancellation
     */
    List<DomainRefundTransactionEntity> findByCancellationId(String cancellationId);
    
    /**
     * Find all refund transactions for a specific return.
     * 
     * @param returnId the ID of the return
     * @return list of refund transaction entities for the return
     */
    List<DomainRefundTransactionEntity> findByReturnId(String returnId);
}