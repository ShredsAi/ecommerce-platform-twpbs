package ai.shreds.domain.ports;

import ai.shreds.domain.entities.DomainPaymentDetailsEntity;
import java.util.Optional;
import java.util.UUID;

/**
 * Domain output port for payment details repository operations.
 * This port defines the contract for payment details persistence operations.
 */
public interface DomainOutputPortPaymentDetailsRepository {

    /**
     * Saves payment details entity.
     * @param paymentDetails The payment details entity to save
     * @return The saved payment details entity
     * @throws ai.shreds.domain.exceptions.DomainValidationException if payment data is invalid
     */
    DomainPaymentDetailsEntity save(DomainPaymentDetailsEntity paymentDetails);

    /**
     * Finds payment details by order ID.
     * @param orderId The order ID to search for
     * @return Optional containing the payment details if found, empty otherwise
     */
    Optional<DomainPaymentDetailsEntity> findByOrderId(UUID orderId);

    /**
     * Finds payment details by transaction ID.
     * @param transactionId The transaction ID to search for
     * @return Optional containing the payment details if found, empty otherwise
     */
    Optional<DomainPaymentDetailsEntity> findByTransactionId(String transactionId);

    /**
     * Checks if payment details exist for the given order ID.
     * @param orderId The order ID to check
     * @return true if payment details exist, false otherwise
     */
    boolean existsByOrderId(UUID orderId);

    /**
     * Deletes payment details by order ID.
     * @param orderId The order ID to delete payment details for
     */
    void deleteByOrderId(UUID orderId);
}