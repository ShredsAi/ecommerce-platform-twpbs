package ai.shreds.infrastructure.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import ai.shreds.domain.entities.DomainPaymentDetailsEntity;
import java.util.Optional;
import java.util.UUID;

public interface InfrastructurePaymentDetailsJpaRepository extends JpaRepository<DomainPaymentDetailsEntity, UUID> {

    Optional<DomainPaymentDetailsEntity> findByOrderId(UUID orderId);
}