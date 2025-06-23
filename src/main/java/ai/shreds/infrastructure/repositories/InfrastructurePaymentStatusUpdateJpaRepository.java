package ai.shreds.infrastructure.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * JPA repository for payment status updates.
 */
@Repository
public interface InfrastructurePaymentStatusUpdateJpaRepository extends JpaRepository<InfrastructurePaymentStatusUpdateJpaEntity, Long> {
    
    /**
     * Find all status updates for a payment, ordered by update time descending.
     *
     * @param paymentId the payment ID
     * @return list of status updates for the payment, most recent first
     */
    List<InfrastructurePaymentStatusUpdateJpaEntity> findByPaymentIdOrderByUpdatedAtDesc(UUID paymentId);
    
    /**
     * Find all status updates for a payment intent, ordered by update time descending.
     *
     * @param intentId the payment intent ID
     * @return list of status updates for the payment intent, most recent first
     */
    List<InfrastructurePaymentStatusUpdateJpaEntity> findByIntentIdOrderByUpdatedAtDesc(UUID intentId);
    
    /**
     * Find status updates by processor type within a time range.
     *
     * @param processorType the processor type (e.g., "STRIPE")
     * @param from the start date/time
     * @param to the end date/time
     * @return list of status updates for the processor within the time range
     */
    List<InfrastructurePaymentStatusUpdateJpaEntity> findByProcessorTypeAndUpdatedAtBetween(
        String processorType, 
        LocalDateTime from, 
        LocalDateTime to
    );
    
    /**
     * Find status updates by status and processor type.
     *
     * @param status the payment status
     * @param processorType the processor type
     * @return list of matching status updates
     */
    List<InfrastructurePaymentStatusUpdateJpaEntity> findByStatusAndProcessorType(String status, String processorType);
}