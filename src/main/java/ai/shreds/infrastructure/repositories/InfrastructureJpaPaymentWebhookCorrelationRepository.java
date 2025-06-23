package ai.shreds.infrastructure.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import ai.shreds.domain.entities.DomainEntityPaymentWebhookCorrelation;
import java.util.Optional;
import java.util.UUID;

public interface InfrastructureJpaPaymentWebhookCorrelationRepository extends JpaRepository<DomainEntityPaymentWebhookCorrelation, UUID> {
    Optional<DomainEntityPaymentWebhookCorrelation> findByWebhookId(UUID webhookId);
}