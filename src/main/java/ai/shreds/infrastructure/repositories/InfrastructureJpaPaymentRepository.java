package ai.shreds.infrastructure.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import ai.shreds.domain.entities.DomainEntityPayment;
import java.util.Optional;
import java.util.UUID;

public interface InfrastructureJpaPaymentRepository extends JpaRepository<DomainEntityPayment, UUID> {
    Optional<DomainEntityPayment> findByProcessorTransactionId(String processorTransactionId);
}