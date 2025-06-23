package ai.shreds.domain.ports;

import ai.shreds.domain.entities.DomainPaymentEntity;
import ai.shreds.domain.entities.DomainPaymentIntentEntity;
import ai.shreds.domain.entities.DomainPaymentStatusUpdateEntity;
import ai.shreds.domain.value_objects.DomainPaymentIdValue;
import ai.shreds.domain.value_objects.DomainPaymentIntentIdValue;

import java.util.List;

/**
 * Output port for payment repository operations.
 * This interface defines the contract for persisting and retrieving payment-related entities.
 */
public interface DomainOutputPortPaymentRepository {

    /**
     * Saves a payment intent entity.
     * @param intent the payment intent to save
     * @return the saved payment intent entity
     */
    DomainPaymentIntentEntity savePaymentIntent(DomainPaymentIntentEntity intent);

    /**
     * Finds a payment intent by its ID.
     * @param id the payment intent ID
     * @return the payment intent entity, or null if not found
     */
    DomainPaymentIntentEntity findPaymentIntentById(DomainPaymentIntentIdValue id);

    /**
     * Saves a payment entity.
     * @param payment the payment to save
     * @return the saved payment entity
     */
    DomainPaymentEntity savePayment(DomainPaymentEntity payment);

    /**
     * Finds a payment by its ID.
     * @param id the payment ID
     * @return the payment entity, or null if not found
     */
    DomainPaymentEntity findPaymentById(DomainPaymentIdValue id);

    /**
     * Finds all payment intents that have expired.
     * @return list of expired payment intents
     */
    List<DomainPaymentIntentEntity> findExpiredIntents();

    /**
     * Saves a payment status update entity.
     * @param update the status update to save
     */
    void saveStatusUpdate(DomainPaymentStatusUpdateEntity update);

    /**
     * Finds a payment by its payment intent ID.
     * @param intentId the payment intent ID
     * @return the payment entity, or null if not found
     */
    DomainPaymentEntity findPaymentByIntentId(DomainPaymentIntentIdValue intentId);

    /**
     * Checks if a payment intent exists.
     * @param id the payment intent ID
     * @return true if the payment intent exists, false otherwise
     */
    boolean existsPaymentIntentById(DomainPaymentIntentIdValue id);

    /**
     * Checks if a payment exists.
     * @param id the payment ID
     * @return true if the payment exists, false otherwise
     */
    boolean existsPaymentById(DomainPaymentIdValue id);

    /**
     * Finds all status updates for a specific payment.
     * @param paymentId the payment ID
     * @return list of status updates for the payment
     */
    List<DomainPaymentStatusUpdateEntity> findStatusUpdatesByPaymentId(DomainPaymentIdValue paymentId);
}