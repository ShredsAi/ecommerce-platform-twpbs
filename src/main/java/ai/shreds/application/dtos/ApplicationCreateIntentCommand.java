package ai.shreds.application.dtos;

import java.util.UUID;
import ai.shreds.shared.value_objects.SharedMoneyValue;

/**
 * Command DTO for creating payment intents in the application layer
 */
public class ApplicationCreateIntentCommand {

    private UUID orderId;
    private UUID customerId;
    private SharedMoneyValue amount;
    private UUID paymentMethodId;

    public ApplicationCreateIntentCommand() {}

    public ApplicationCreateIntentCommand(UUID orderId, UUID customerId, SharedMoneyValue amount, UUID paymentMethodId) {
        this.orderId = orderId;
        this.customerId = customerId;
        this.amount = amount;
        this.paymentMethodId = paymentMethodId;
    }

    public UUID getOrderId() {
        return orderId;
    }

    public void setOrderId(UUID orderId) {
        this.orderId = orderId;
    }

    public UUID getCustomerId() {
        return customerId;
    }

    public void setCustomerId(UUID customerId) {
        this.customerId = customerId;
    }

    public SharedMoneyValue getAmount() {
        return amount;
    }

    public void setAmount(SharedMoneyValue amount) {
        this.amount = amount;
    }

    public UUID getPaymentMethodId() {
        return paymentMethodId;
    }

    public void setPaymentMethodId(UUID paymentMethodId) {
        this.paymentMethodId = paymentMethodId;
    }
}