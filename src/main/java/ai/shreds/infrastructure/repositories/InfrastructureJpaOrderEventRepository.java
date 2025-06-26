package ai.shreds.infrastructure.repositories;

import ai.shreds.domain.entities.DomainOrderEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * JPA repository for order event entities.
 */
@Repository
public interface InfrastructureJpaOrderEventRepository extends JpaRepository<DomainOrderEventEntity, String> {

    List<DomainOrderEventEntity> findByOrderIdOrderByTimestampDesc(String orderId);

    List<DomainOrderEventEntity> findByEventType(String eventType);
}