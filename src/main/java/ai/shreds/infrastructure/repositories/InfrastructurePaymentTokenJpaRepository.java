package ai.shreds.infrastructure.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * JPA repository for payment tokens.
 */
@Repository
public interface InfrastructurePaymentTokenJpaRepository extends JpaRepository<InfrastructurePaymentTokenJpaEntity, UUID> {
    
    /**
     * Find a payment token by payment method ID.
     *
     * @param paymentMethodId the payment method ID
     * @return the payment token, if exists
     */
    Optional<InfrastructurePaymentTokenJpaEntity> findByPaymentMethodId(UUID paymentMethodId);
    
    /**
     * Find all payment tokens for a specific processor type.
     *
     * @param processorType the processor type (e.g., "STRIPE")
     * @return list of payment tokens for the processor
     */
    List<InfrastructurePaymentTokenJpaEntity> findByProcessorType(String processorType);
    
    /**
     * Find all expired payment tokens.
     *
     * @param dateTime the reference date/time
     * @return list of expired payment tokens
     */
    List<InfrastructurePaymentTokenJpaEntity> findByExpiresAtBefore(LocalDateTime dateTime);
}