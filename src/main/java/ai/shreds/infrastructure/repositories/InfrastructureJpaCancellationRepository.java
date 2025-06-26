package ai.shreds.infrastructure.repositories;

import ai.shreds.domain.entities.DomainCancellationRequestEntity;
import ai.shreds.shared.enums.SharedCancellationStatusEnum;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * JPA repository for cancellation request entities.
 */
@Repository
public interface InfrastructureJpaCancellationRepository extends JpaRepository<DomainCancellationRequestEntity, String> {

    Optional<DomainCancellationRequestEntity> findById(String id);

    List<DomainCancellationRequestEntity> findByOrderId(String orderId);

    List<DomainCancellationRequestEntity> findByStatus(SharedCancellationStatusEnum status);

    List<DomainCancellationRequestEntity> findByStatusAndRequestedAtBefore(SharedCancellationStatusEnum status, LocalDateTime cutoff);
}