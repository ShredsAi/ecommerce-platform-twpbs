package ai.shreds.infrastructure.repositories;

import ai.shreds.domain.entities.DomainEntityPaymentStatusUpdate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InfrastructureJpaPaymentStatusUpdateRepository extends JpaRepository<DomainEntityPaymentStatusUpdate, Long> {
    List<DomainEntityPaymentStatusUpdate> findByProcessedFalseOrderByUpdatedAt();

    @Modifying
    @Query("update DomainEntityPaymentStatusUpdate d set d.processed = :processed where d.id = :id")
    void updateProcessedById(@Param("id") Long id, @Param("processed") Boolean processed);
}
