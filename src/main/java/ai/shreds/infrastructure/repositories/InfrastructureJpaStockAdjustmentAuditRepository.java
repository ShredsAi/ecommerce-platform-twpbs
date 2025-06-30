package ai.shreds.infrastructure.repositories;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import ai.shreds.infrastructure.entities.InfrastructureJpaEntityStockAdjustmentAudit;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface InfrastructureJpaStockAdjustmentAuditRepository extends JpaRepository<InfrastructureJpaEntityStockAdjustmentAudit, UUID> {

    /**
     * Finds audit records by ledger ID
     */
    List<InfrastructureJpaEntityStockAdjustmentAudit> findByLedgerId(UUID ledgerId);
    
    /**
     * Finds audit records by SKU ID and location ID
     */
    List<InfrastructureJpaEntityStockAdjustmentAudit> findBySkuIdAndLocationId(String skuId, String locationId, Pageable pageable);
    
    /**
     * Finds audit records by reason
     */
    List<InfrastructureJpaEntityStockAdjustmentAudit> findByReason(String reason, Pageable pageable);
    
    /**
     * Finds audit records created after a specific time
     */
    @Query("SELECT a FROM InfrastructureJpaEntityStockAdjustmentAudit a WHERE a.createdAt >= :since ORDER BY a.createdAt DESC")
    List<InfrastructureJpaEntityStockAdjustmentAudit> findRecentAudits(@Param("since") Instant since, Pageable pageable);
    
    /**
     * Finds audit records by source
     */
    List<InfrastructureJpaEntityStockAdjustmentAudit> findBySource(String source, Pageable pageable);
    
    /**
     * Finds audit records by user ID
     */
    List<InfrastructureJpaEntityStockAdjustmentAudit> findByUserId(String userId, Pageable pageable);
    
    /**
     * Counts adjustments by reason
     */
    long countByReason(String reason);
}