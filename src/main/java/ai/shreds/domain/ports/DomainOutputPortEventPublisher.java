package ai.shreds.domain.ports;

import ai.shreds.shared.enums.SharedOrderStatusEnum;
import java.util.UUID;

/**
 * Domain output port for event publishing operations.
 * This port defines the contract for publishing domain events to external systems.
 */
public interface DomainOutputPortEventPublisher {

    /**
     * Publishes an order status change event.
     * @param orderId The order ID that changed status
     * @param previousStatus The previous order status
     * @param newStatus The new order status
     * @throws ai.shreds.domain.exceptions.DomainSagaException if event publishing fails
     */
    void publishOrderStatusChanged(UUID orderId, SharedOrderStatusEnum previousStatus, SharedOrderStatusEnum newStatus);

    /**
     * Publishes a fulfillment event for saga coordination.
     * @param sagaId The saga ID
     * @param orderId The order ID
     * @param step The current saga step
     * @param status The step status
     * @throws ai.shreds.domain.exceptions.DomainSagaException if event publishing fails
     */
    void publishFulfillmentEvent(UUID sagaId, UUID orderId, String step, String status);

    /**
     * Publishes a payment event.
     * @param orderId The order ID
     * @param transactionId The payment transaction ID
     * @param paymentStatus The payment status
     * @param amount The payment amount as string representation
     */
    void publishPaymentEvent(UUID orderId, String transactionId, String paymentStatus, String amount);

    /**
     * Publishes a shipping event.
     * @param orderId The order ID
     * @param trackingNumber The shipment tracking number
     * @param shippingStatus The shipping status
     * @param carrier The shipping carrier
     */
    void publishShippingEvent(UUID orderId, String trackingNumber, String shippingStatus, String carrier);

    /**
     * Publishes a saga timeout event.
     * @param sagaId The saga ID that timed out
     * @param orderId The associated order ID
     * @param currentStep The step that timed out
     * @param retryCount The number of retries attempted
     */
    void publishSagaTimeoutEvent(UUID sagaId, UUID orderId, String currentStep, int retryCount);
}