package ai.shreds.domain.ports;

import ai.shreds.domain.entities.DomainEntityPaymentStatusUpdate;
import java.util.List;

/**
 * Port for managing payment status updates.
 * Implemented by the infrastructure layer.
 */
public interface DomainOutputPortStatusUpdateRepository {
    /**
     * Finds all unprocessed payment status updates.
     *
     * @return List of unprocessed status updates
     */
    List<DomainEntityPaymentStatusUpdate> findUnprocessedUpdates();

    /**
     * Marks a status update as processed.
     *
     * @param id The ID of the status update to mark as processed
     */
    void markAsProcessed(Long id);
}
