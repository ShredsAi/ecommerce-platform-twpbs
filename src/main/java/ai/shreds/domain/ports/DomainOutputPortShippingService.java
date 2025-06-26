package ai.shreds.domain.ports;

import ai.shreds.domain.entities.DomainOrderEntity;
import ai.shreds.shared.dtos.ShipmentResult;
import java.util.UUID;

/**
 * Domain output port for shipping service operations.
 * This port defines the contract for shipping and logistics operations.
 */
public interface DomainOutputPortShippingService {

    /**
     * Creates a shipment for an order.
     * @param order The order to create shipment for
     * @return ShipmentResult containing shipment details
     * @throws ai.shreds.domain.exceptions.DomainSagaException if shipment creation fails
     */
    ShipmentResult createShipment(DomainOrderEntity order);

    /**
     * Cancels a shipment by tracking number.
     * @param trackingNumber The tracking number of shipment to cancel
     * @return true if cancellation was successful, false otherwise
     * @throws ai.shreds.domain.exceptions.DomainSagaException if cancellation fails
     */
    boolean cancelShipment(String trackingNumber);

    /**
     * Subscribes to shipping updates for an order.
     * @param orderId The order ID to subscribe to updates for
     * @throws ai.shreds.domain.exceptions.DomainSagaException if subscription fails
     */
    void subscribeToUpdates(UUID orderId);

    /**
     * Gets the current shipping status for a tracking number.
     * @param trackingNumber The tracking number to check
     * @return ShipmentResult containing current status
     */
    ShipmentResult getShipmentStatus(String trackingNumber);

    /**
     * Schedules expedited shipping for an order.
     * @param order The order to expedite shipping for
     * @return ShipmentResult containing expedited shipment details
     */
    ShipmentResult scheduleExpeditedShipping(DomainOrderEntity order);
}