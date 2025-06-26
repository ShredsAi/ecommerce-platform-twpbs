package ai.shreds.infrastructure.repositories;

import ai.shreds.domain.entities.DomainOrderEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InfrastructureJpaOrderEventRepository extends JpaRepository<DomainOrderEventEntity, String> {

    Optional<DomainOrderEventEntity> findById(String id);

    @Query("SELECT e FROM DomainOrderEventEntity e WHERE e.orderId = :orderId ORDER BY e.timestamp DESC")
    List<DomainOrderEventEntity> findByOrderIdOrderByTimestampDesc(@Param("orderId") String orderId);

    @Query("SELECT e FROM DomainOrderEventEntity e WHERE e.eventType = :eventType")
    List<DomainOrderEventEntity> findByEventType(@Param("eventType") String eventType);

    @Query("SELECT e FROM DomainOrderEventEntity e WHERE e.orderId = :orderId AND e.eventType = :eventType ORDER BY e.timestamp DESC")
    List<DomainOrderEventEntity> findByOrderIdAndEventType(@Param("orderId") String orderId, @Param("eventType") String eventType);

    @Query("SELECT e FROM DomainOrderEventEntity e WHERE e.correlationId = :correlationId ORDER BY e.timestamp DESC")
    List<DomainOrderEventEntity> findByCorrelationId(@Param("correlationId") String correlationId);
}