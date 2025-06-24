package ai.shreds.infrastructure.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import ai.shreds.domain.entities.DomainOrderItemEntity;
import java.util.List;
import java.util.UUID;

public interface InfrastructureOrderItemJpaRepository extends JpaRepository<DomainOrderItemEntity, UUID> {

    List<DomainOrderItemEntity> findAllByOrderId(UUID orderId);

    void deleteAllByOrderId(UUID orderId);
}