package ai.shreds.infrastructure.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import ai.shreds.domain.entities.DomainEntityPaymentWebhook;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InfrastructureJpaPaymentWebhookRepository extends JpaRepository<DomainEntityPaymentWebhook, UUID> {
    Optional<DomainEntityPaymentWebhook> findByExternalEventIdAndProcessorType(String externalEventId, String processorType);
    List<DomainEntityPaymentWebhook> findByProcessingStatus(String processingStatus);
}