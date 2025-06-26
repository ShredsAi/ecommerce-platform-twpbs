package ai.shreds.infrastructure.repositories;

import ai.shreds.domain.entities.DomainCancellationRequestEntity;
import ai.shreds.shared.enums.SharedCancellationStatusEnum;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface InfrastructureJpaCancellationRepository extends JpaRepository<DomainCancellationRequestEntity, String> {

    Optional<DomainCancellationRequestEntity> findById(String id);

    @Query("SELECT c FROM DomainCancellationRequestEntity c WHERE c.orderId.value = :orderId")
    List<DomainCancellationRequestEntity> findByOrderId(@Param("orderId") String orderId);

    @Query("SELECT c FROM DomainCancellationRequestEntity c WHERE c.status = :status")
    List<DomainCancellationRequestEntity> findByStatus(@Param("status") SharedCancellationStatusEnum status);

    @Query("SELECT c FROM DomainCancellationRequestEntity c WHERE c.status = :status AND c.requestedAt < :cutoff")
    List<DomainCancellationRequestEntity> findByStatusAndRequestedAtBefore(
            @Param("status") SharedCancellationStatusEnum status,
            @Param("cutoff") LocalDateTime cutoff);

    @Query("SELECT c FROM DomainCancellationRequestEntity c WHERE c.customerId.value = :customerId")
    List<DomainCancellationRequestEntity> findByCustomerId(@Param("customerId") String customerId);

    @Query("SELECT c FROM DomainCancellationRequestEntity c WHERE c.status = 'PENDING' AND c.requestedAt < :cutoff")
    List<DomainCancellationRequestEntity> findPendingBefore(@Param("cutoff") LocalDateTime cutoff);
}