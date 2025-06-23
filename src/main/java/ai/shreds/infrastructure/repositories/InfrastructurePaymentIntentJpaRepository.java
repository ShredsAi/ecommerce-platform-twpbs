package ai.shreds.infrastructure.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;
import java.time.LocalDateTime;

@Repository
public interface InfrastructurePaymentIntentJpaRepository extends JpaRepository<InfrastructurePaymentIntentJpaEntity, UUID> {
    List<InfrastructurePaymentIntentJpaEntity> findByExpiresAtBefore(LocalDateTime dateTime);
}