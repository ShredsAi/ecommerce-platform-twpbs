package ai.shreds.domain.ports;

import ai.shreds.shared.enums.SharedCancellationReasonEnum;
import ai.shreds.shared.dtos.SharedOrderSnapshotDTO;
import ai.shreds.shared.dtos.SharedOrderItemDTO;

import java.util.List;

/**
 * Domain input port for eligibility checking operations.
 * Defines the contract for determining whether cancellations and returns are allowed.
 */
public interface DomainInputPortEligibilityChecker {
    
    /**
     * Check if a cancellation is allowed for the given order and reason.
     * 
     * @param orderSnapshot the order snapshot containing order details
     * @param reason the reason for cancellation
     * @return true if cancellation is allowed, false otherwise
     */
    boolean isCancellationAllowed(SharedOrderSnapshotDTO orderSnapshot, SharedCancellationReasonEnum reason);
    
    /**
     * Check if a return is allowed for the given order and items.
     * 
     * @param orderSnapshot the order snapshot containing order details
     * @param items the list of items to be returned
     * @return true if return is allowed, false otherwise
     */
    boolean isReturnAllowed(SharedOrderSnapshotDTO orderSnapshot, List<SharedOrderItemDTO> items);
    
    /**
     * Get the reason why a cancellation was rejected.
     * 
     * @param orderSnapshot the order snapshot containing order details
     * @return the rejection reason, or null if cancellation is allowed
     */
    String getCancellationRejectionReason(SharedOrderSnapshotDTO orderSnapshot);
    
    /**
     * Get the reason why a return was rejected.
     * 
     * @param orderSnapshot the order snapshot containing order details
     * @return the rejection reason, or null if return is allowed
     */
    String getReturnRejectionReason(SharedOrderSnapshotDTO orderSnapshot);
}