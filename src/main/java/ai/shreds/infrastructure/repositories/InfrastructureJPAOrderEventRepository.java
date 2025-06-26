package ai.shreds.infrastructure.repositories;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface InfrastructureJPAOrderEventRepository extends JpaRepository<InfrastructureOrderEventJPAEntity, UUID> {
    List<InfrastructureOrderEventJPAEntity> findByOrderIdOrderByEventTimestampDesc(UUID orderId, Pageable pageable);
    long countByOrderId(UUID orderId);
}
