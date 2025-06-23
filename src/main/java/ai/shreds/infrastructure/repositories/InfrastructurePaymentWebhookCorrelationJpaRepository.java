package ai.shreds.infrastructure.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * JPA repository for payment webhook correlations.
 */
@Repository
public interface InfrastructurePaymentWebhookCorrelationJpaRepository extends JpaRepository<InfrastructurePaymentWebhookJpaEntity, Long> {
    
    /**
     * Find a webhook correlation by webhook ID.
     *
     * @param webhookId the webhook ID from the external processor
     * @return the webhook correlation, if exists
     */
    Optional<InfrastructurePaymentWebhookJpaEntity> findByWebhookId(String webhookId);
    
    /**
     * Find all webhook correlations for a specific payment.
     *
     * @param paymentId the payment ID
     * @return list of webhook correlations for the payment
     */
    List<InfrastructurePaymentWebhookJpaEntity> findByPaymentId(UUID paymentId);
    
    /**
     * Find all unprocessed webhook correlations.
     *
     * @param processed whether the webhook has been processed
     * @return list of unprocessed webhook correlations
     */
    List<InfrastructurePaymentWebhookJpaEntity> findByProcessed(boolean processed);
    
    /**
     * Find unprocessed webhook correlations older than specified time.
     * This is useful for finding stuck webhooks that need retry.
     *
     * @param processed whether the webhook has been processed
     * @param receivedBefore the cutoff time
     * @return list of old unprocessed webhook correlations
     */
    List<InfrastructurePaymentWebhookJpaEntity> findByProcessedAndReceivedAtBefore(boolean processed, LocalDateTime receivedBefore);
    
    /**
     * Find webhook correlations received within a time range.
     *
     * @param from the start date/time
     * @param to the end date/time
     * @return list of webhook correlations within the time range
     */
    List<InfrastructurePaymentWebhookJpaEntity> findByReceivedAtBetween(LocalDateTime from, LocalDateTime to);
}