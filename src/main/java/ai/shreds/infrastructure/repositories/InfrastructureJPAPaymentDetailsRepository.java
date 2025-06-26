package ai.shreds.infrastructure.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.UUID;
import java.util.Optional;

@Repository
public interface InfrastructureJPAPaymentDetailsRepository extends JpaRepository<InfrastructurePaymentDetailsJPAEntity, UUID> {
    Optional<InfrastructurePaymentDetailsJPAEntity> findByOrderId(UUID orderId);
}
