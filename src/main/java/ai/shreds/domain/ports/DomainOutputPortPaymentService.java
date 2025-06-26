package ai.shreds.domain.ports;

import ai.shreds.domain.entities.DomainOrderEntity;
import ai.shreds.shared.dtos.PaymentResult;
import ai.shreds.shared.value_objects.SharedMoneyValue;

/**
 * Domain output port for payment service operations.
 * This port defines the contract for payment processing operations.
 */
public interface DomainOutputPortPaymentService {

    /**
     * Authorizes payment for an order.
     * @param order The order to authorize payment for
     * @return PaymentResult containing authorization details
     * @throws ai.shreds.domain.exceptions.DomainSagaException if authorization fails
     */
    PaymentResult authorize(DomainOrderEntity order);

    /**
     * Captures a previously authorized payment.
     * @param transactionId The transaction ID to capture
     * @param amount The amount to capture
     * @return PaymentResult containing capture details
     * @throws ai.shreds.domain.exceptions.DomainSagaException if capture fails
     */
    PaymentResult capture(String transactionId, SharedMoneyValue amount);

    /**
     * Refunds a previously captured payment.
     * @param transactionId The transaction ID to refund
     * @param amount The amount to refund
     * @return PaymentResult containing refund details
     * @throws ai.shreds.domain.exceptions.DomainSagaException if refund fails
     */
    PaymentResult refund(String transactionId, SharedMoneyValue amount);

    /**
     * Cancels a payment authorization.
     * @param transactionId The transaction ID to cancel
     * @return PaymentResult containing cancellation details
     */
    PaymentResult cancel(String transactionId);

    /**
     * Checks the status of a payment transaction.
     * @param transactionId The transaction ID to check
     * @return PaymentResult containing current status
     */
    PaymentResult checkStatus(String transactionId);
}