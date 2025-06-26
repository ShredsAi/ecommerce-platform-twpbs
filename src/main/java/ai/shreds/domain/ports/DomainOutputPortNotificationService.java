package ai.shreds.domain.ports;

import java.util.Map;

/**
 * Domain output port for notification service operations.
 * This port defines the contract for sending notifications to customers.
 */
public interface DomainOutputPortNotificationService {

    /**
     * Sends a notification to a customer.
     * @param customerId The customer ID to send notification to
     * @param notificationType The type of notification (ORDER_CONFIRMED, ORDER_SHIPPED, etc.)
     * @param data Additional data for the notification
     * @throws ai.shreds.domain.exceptions.DomainSagaException if notification sending fails
     */
    void notifyCustomer(String customerId, String notificationType, Map<String, Object> data);

    /**
     * Sends an order confirmation notification.
     * @param customerId The customer ID
     * @param orderId The order ID
     * @param orderDetails Additional order details
     */
    void sendOrderConfirmation(String customerId, String orderId, Map<String, Object> orderDetails);

    /**
     * Sends a payment confirmation notification.
     * @param customerId The customer ID
     * @param orderId The order ID
     * @param paymentDetails Payment details
     */
    void sendPaymentConfirmation(String customerId, String orderId, Map<String, Object> paymentDetails);

    /**
     * Sends a shipping notification.
     * @param customerId The customer ID
     * @param orderId The order ID
     * @param shippingDetails Shipping details including tracking number
     */
    void sendShippingNotification(String customerId, String orderId, Map<String, Object> shippingDetails);

    /**
     * Sends a delivery confirmation notification.
     * @param customerId The customer ID
     * @param orderId The order ID
     * @param deliveryDetails Delivery confirmation details
     */
    void sendDeliveryConfirmation(String customerId, String orderId, Map<String, Object> deliveryDetails);

    /**
     * Sends an order cancellation notification.
     * @param customerId The customer ID
     * @param orderId The order ID
     * @param cancellationReason The reason for cancellation
     */
    void sendCancellationNotification(String customerId, String orderId, String cancellationReason);
}