package ai.shreds.infrastructure.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import ai.shreds.domain.entities.DomainOrderEntity;
import java.util.Optional;
import java.util.UUID;

public interface InfrastructureOrderJpaRepository extends JpaRepository<DomainOrderEntity, UUID> {

    Optional<DomainOrderEntity> findByCartId(String cartId);

    Optional<DomainOrderEntity> findByOrderNumber(String orderNumber);
}