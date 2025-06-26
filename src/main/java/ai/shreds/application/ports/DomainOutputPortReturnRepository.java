package ai.shreds.application.ports;

import java.util.List;
import ai.shreds.domain.entities.DomainReturnRequestEntity;
import ai.shreds.shared.enums.SharedReturnStatusEnum;

/**
 * Output port for persistence operations on return requests.
 */
public interface DomainOutputPortReturnRepository {
    /**
     * Save or update a return request.
     * @param returnRequest the domain return request entity
     * @return the persisted entity
     */
    DomainReturnRequestEntity save(DomainReturnRequestEntity returnRequest);

    /**
     * Find a return request by its unique identifier.
     * @param returnId the return request ID
     * @return the domain return request entity or null if not found
     */
    DomainReturnRequestEntity findById(String returnId);

    /**
     * Find all return requests for a given order.
     * @param orderId the order identifier
     * @return list of matching return request entities
     */
    List<DomainReturnRequestEntity> findByOrderId(String orderId);

    /**
     * Find all return requests by status.
     * @param status the return status
     * @return list of matching return request entities
     */
    List<DomainReturnRequestEntity> findByReturnStatus(SharedReturnStatusEnum status);

    /**
     * Find a return request by its RMA number.
     * @param rmaNumber the return merchandise authorization number
     * @return the domain return request entity or null if not found
     */
    DomainReturnRequestEntity findByRmaNumber(String rmaNumber);
}