package ai.shreds.domain.ports;

import ai.shreds.domain.entities.DomainEntityPayment;
import java.util.UUID;

/**
 * Port for querying payment data from payment repository.
 * Implemented by the infrastructure layer.
 */
public interface DomainOutputPortPaymentQuery {
    /**
     * Finds a payment by its processor transaction ID.
     *
     * @param processorTransactionId The payment processor's transaction ID
     * @return The payment entity if found, null otherwise
     */
    DomainEntityPayment findPaymentByProcessorTransactionId(String processorTransactionId);
    
    /**
     * Finds a payment by its internal payment ID.
     *
     * @param paymentId The internal payment ID
     * @return The payment entity if found, null otherwise
     */
    DomainEntityPayment findPaymentById(UUID paymentId);
}
