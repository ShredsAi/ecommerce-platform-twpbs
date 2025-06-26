package ai.shreds.infrastructure.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface InfrastructureJPAShippingDetailsRepository extends JpaRepository<InfrastructureShippingDetailsJPAEntity, UUID> {
    Optional<InfrastructureShippingDetailsJPAEntity> findByOrderId(UUID orderId);
}
