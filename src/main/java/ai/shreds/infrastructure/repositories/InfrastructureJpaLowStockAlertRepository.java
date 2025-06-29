package ai.shreds.infrastructure.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import ai.shreds.infrastructure.entities.InfrastructureJpaEntityLowStockAlert;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface InfrastructureJpaLowStockAlertRepository extends JpaRepository<InfrastructureJpaEntityLowStockAlert, UUID> {

    /**
     * Finds an alert by SKU ID, location ID, and status
     */
    @Query("SELECT a FROM InfrastructureJpaEntityLowStockAlert a WHERE a.skuId = :skuId AND a.locationId = :locationId AND a.status = :status")
    Optional<InfrastructureJpaEntityLowStockAlert> findBySkuIdAndLocationIdAndStatus(
        @Param("skuId") String skuId, 
        @Param("locationId") String locationId, 
        @Param("status") String status);

    /**
     * Finds all alerts by rule ID
     */
    List<InfrastructureJpaEntityLowStockAlert> findByRuleId(UUID ruleId);

    /**
     * Finds all alerts by SKU ID
     */
    List<InfrastructureJpaEntityLowStockAlert> findBySkuId(String skuId);

    /**
     * Finds all alerts by location ID
     */
    List<InfrastructureJpaEntityLowStockAlert> findByLocationId(String locationId);

    /**
     * Finds all alerts by status
     */
    List<InfrastructureJpaEntityLowStockAlert> findByStatus(String status);

    /**
     * Finds all alerts created after a specific timestamp
     */
    @Query("SELECT a FROM InfrastructureJpaEntityLowStockAlert a WHERE a.createdAt >= :since ORDER BY a.createdAt DESC")
    List<InfrastructureJpaEntityLowStockAlert> findRecentAlerts(@Param("since") Instant since);

    /**
     * Counts alerts by status
     */
    @Query("SELECT COUNT(a) FROM InfrastructureJpaEntityLowStockAlert a WHERE a.status = :status")
    long countByStatus(@Param("status") String status);

    /**
     * Finds unresolved alerts (PENDING or ACKNOWLEDGED)
     */
    @Query("SELECT a FROM InfrastructureJpaEntityLowStockAlert a WHERE a.status IN ('PENDING', 'ACKNOWLEDGED') ORDER BY a.createdAt DESC")
    List<InfrastructureJpaEntityLowStockAlert> findUnresolvedAlerts();
}