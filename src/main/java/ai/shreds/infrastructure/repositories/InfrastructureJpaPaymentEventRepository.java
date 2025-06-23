package ai.shreds.infrastructure.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import ai.shreds.domain.entities.DomainEntityPaymentEvent;
import java.util.List;
import java.util.UUID;

public interface InfrastructureJpaPaymentEventRepository extends JpaRepository<DomainEntityPaymentEvent, UUID> {
    List<DomainEntityPaymentEvent> findByPaymentId(UUID paymentId);
}