package ai.shreds.infrastructure.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface InfrastructurePaymentJpaRepository extends JpaRepository<InfrastructurePaymentJpaEntity, UUID> {
    Optional<InfrastructurePaymentJpaEntity> findByPaymentIntentId(UUID paymentIntentId);
}