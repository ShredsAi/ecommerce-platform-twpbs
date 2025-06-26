package ai.shreds.infrastructure.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface InfrastructureJPASagaStateRepository extends JpaRepository<InfrastructureSagaStateJPAEntity, UUID> {
    List<InfrastructureSagaStateJPAEntity> findByStatusAndLastActivityBefore(String status, Instant cutoff);
    java.util.Optional<InfrastructureSagaStateJPAEntity> findByOrderId(UUID orderId);
}
