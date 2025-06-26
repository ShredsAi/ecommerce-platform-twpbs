package ai.shreds.domain.ports;

import ai.shreds.domain.entities.DomainShippingDetailsEntity;
import ai.shreds.shared.enums.SharedShippingStatusEnum;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Domain output port for shipping details repository operations.
 * This port defines the contract for shipping details persistence operations.
 */
public interface DomainOutputPortShippingDetailsRepository {

    /**
     * Saves shipping details entity.
     * @param shippingDetails The shipping details entity to save
     * @return The saved shipping details entity
     * @throws ai.shreds.domain.exceptions.DomainValidationException if shipping data is invalid
     */
    DomainShippingDetailsEntity save(DomainShippingDetailsEntity shippingDetails);

    /**
     * Finds shipping details by order ID.
     * @param orderId The order ID to search for
     * @return Optional containing the shipping details if found, empty otherwise
     */
    Optional<DomainShippingDetailsEntity> findByOrderId(UUID orderId);

    /**
     * Finds shipping details by tracking number.
     * @param trackingNumber The tracking number to search for
     * @return Optional containing the shipping details if found, empty otherwise
     */
    Optional<DomainShippingDetailsEntity> findByTrackingNumber(String trackingNumber);

    /**
     * Finds all shipping details with specific status.
     * @param status The shipping status to filter by
     * @return List of shipping details with the given status
     */
    List<DomainShippingDetailsEntity> findByStatus(SharedShippingStatusEnum status);

    /**
     * Checks if shipping details exist for the given order ID.
     * @param orderId The order ID to check
     * @return true if shipping details exist, false otherwise
     */
    boolean existsByOrderId(UUID orderId);

    /**
     * Deletes shipping details by order ID.
     * @param orderId The order ID to delete shipping details for
     */
    void deleteByOrderId(UUID orderId);
}