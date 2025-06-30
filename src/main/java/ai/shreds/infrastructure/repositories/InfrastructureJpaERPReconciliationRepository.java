package ai.shreds.infrastructure.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import ai.shreds.infrastructure.entities.InfrastructureJpaEntityERPReconciliation;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface InfrastructureJpaERPReconciliationRepository extends JpaRepository<InfrastructureJpaEntityERPReconciliation, UUID> {

    /**
     * Finds a reconciliation record by batch ID
     */
    Optional<InfrastructureJpaEntityERPReconciliation> findByBatchId(String batchId);
    
    /**
     * Finds reconciliation records by status
     */
    List<InfrastructureJpaEntityERPReconciliation> findByStatus(String status);
    
    /**
     * Finds reconciliation records processed after a specific time
     */
    @Query("SELECT r FROM InfrastructureJpaEntityERPReconciliation r WHERE r.processedAt >= :since ORDER BY r.processedAt DESC")
    List<InfrastructureJpaEntityERPReconciliation> findRecentReconciliations(@Param("since") Instant since);
    
    /**
     * Counts reconciliations by status
     */
    @Query("SELECT COUNT(r) FROM InfrastructureJpaEntityERPReconciliation r WHERE r.status = :status")
    long countByStatus(@Param("status") String status);
    
    /**
     * Checks if a batch has been processed
     */
    boolean existsByBatchId(String batchId);
}