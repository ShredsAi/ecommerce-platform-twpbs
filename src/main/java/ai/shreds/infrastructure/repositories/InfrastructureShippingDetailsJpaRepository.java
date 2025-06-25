package ai.shreds.infrastructure.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import ai.shreds.domain.entities.DomainShippingDetailsEntity;
import java.util.Optional;
import java.util.UUID;

public interface InfrastructureShippingDetailsJpaRepository extends JpaRepository<DomainShippingDetailsEntity, UUID> {

    Optional<DomainShippingDetailsEntity> findByOrderId(UUID orderId);
}