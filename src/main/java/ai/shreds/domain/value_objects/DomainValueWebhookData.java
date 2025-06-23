package ai.shreds.domain.value_objects;

import ai.shreds.shared.value_objects.SharedValueMoney;
import java.time.LocalDateTime;

/**
 * Value object containing normalized data extracted from a payment processor webhook.
 * This provides a common structure regardless of which payment processor the webhook came from.
 */
public class DomainValueWebhookData {
    private final String paymentIntentId;
    private final String customerId;
    private final String orderId;
    private final SharedValueMoney amount;
    private final String status;
    private final String processorTransactionId;
    private final LocalDateTime timestamp;

    public DomainValueWebhookData(String paymentIntentId,
                                 String customerId,
                                 String orderId,
                                 SharedValueMoney amount,
                                 String status,
                                 String processorTransactionId,
                                 LocalDateTime timestamp) {
        this.paymentIntentId = paymentIntentId;
        this.customerId = customerId;
        this.orderId = orderId;
        this.amount = amount;
        this.status = status;
        this.processorTransactionId = processorTransactionId;
        this.timestamp = timestamp;
    }

    public String getPaymentIntentId() {
        return paymentIntentId;
    }

    public String getCustomerId() {
        return customerId;
    }

    public String getOrderId() {
        return orderId;
    }

    public SharedValueMoney getAmount() {
        return amount;
    }

    public String getStatus() {
        return status;
    }

    public String getProcessorTransactionId() {
        return processorTransactionId;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }
}
