package ai.shreds.domain.commands;

import ai.shreds.domain.value_objects.DomainOrderIdValue;
import ai.shreds.domain.value_objects.DomainCustomerIdValue;
import ai.shreds.domain.value_objects.DomainMoneyValue;
import ai.shreds.domain.value_objects.DomainPaymentMethodIdValue;

/**
 * Command to create a payment intent in the domain layer.
 */
public class DomainCreateIntentCommand {
    private final DomainOrderIdValue orderId;
    private final DomainCustomerIdValue customerId;
    private final DomainMoneyValue amount;
    private final DomainPaymentMethodIdValue paymentMethodId;

    public DomainCreateIntentCommand(
            DomainOrderIdValue orderId,
            DomainCustomerIdValue customerId,
            DomainMoneyValue amount,
            DomainPaymentMethodIdValue paymentMethodId) {
        this.orderId = orderId;
        this.customerId = customerId;
        this.amount = amount;
        this.paymentMethodId = paymentMethodId;
    }

    public DomainOrderIdValue getOrderId() {
        return orderId;
    }

    public DomainCustomerIdValue getCustomerId() {
        return customerId;
    }

    public DomainMoneyValue getAmount() {
        return amount;
    }

    public DomainPaymentMethodIdValue getPaymentMethodId() {
        return paymentMethodId;
    }
}