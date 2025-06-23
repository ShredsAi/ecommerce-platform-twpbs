package ai.shreds.infrastructure.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * JPA repository for 3D Secure entities.
 */
@Repository
public interface InfrastructureThreeDSecureJpaRepository extends JpaRepository<InfrastructureThreeDSecureJpaEntity, UUID> {
    
    /**
     * Find a 3D Secure session by payment intent ID.
     *
     * @param paymentIntentId the payment intent ID
     * @return the 3D Secure session, if exists
     */
    Optional<InfrastructureThreeDSecureJpaEntity> findByPaymentIntentId(UUID paymentIntentId);
    
    /**
     * Find all 3D Secure sessions with a specific status.
     *
     * @param status the status (e.g., "PENDING", "AUTHENTICATED")
     * @return list of 3D Secure sessions with the specified status
     */
    List<InfrastructureThreeDSecureJpaEntity> findByStatus(String status);
    
    /**
     * Find all expired 3D Secure sessions.
     *
     * @param dateTime the reference date/time
     * @return list of expired 3D Secure sessions
     */
    List<InfrastructureThreeDSecureJpaEntity> findByExpiresAtBefore(LocalDateTime dateTime);
    
    /**
     * Find all 3D Secure sessions in PENDING status that have expired.
     *
     * @param status the status to filter by
     * @param dateTime the reference date/time
     * @return list of expired pending 3D Secure sessions
     */
    List<InfrastructureThreeDSecureJpaEntity> findByStatusAndExpiresAtBefore(String status, LocalDateTime dateTime);
}