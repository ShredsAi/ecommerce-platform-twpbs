package ai.shreds.infrastructure.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import ai.shreds.infrastructure.entities.InfrastructureJpaEntitySafetyStockRule;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface InfrastructureJpaSafetyStockRuleRepository extends JpaRepository<InfrastructureJpaEntitySafetyStockRule, UUID> {

    /**
     * Finds an active safety stock rule by SKU ID and location ID
     */
    Optional<InfrastructureJpaEntitySafetyStockRule> findBySkuIdAndLocationIdAndIsActive(
        String skuId, String locationId, Boolean isActive);

    /**
     * Finds all active safety stock rules
     */
    List<InfrastructureJpaEntitySafetyStockRule> findAllByIsActive(Boolean isActive);

    /**
     * Finds active safety stock rules with pagination using native query
     */
    @Query("SELECT r FROM InfrastructureJpaEntitySafetyStockRule r WHERE r.isActive = true ORDER BY r.createdAt DESC")
    List<InfrastructureJpaEntitySafetyStockRule> findAllActiveWithPagination(int offset, int limit);

    /**
     * Deactivates all safety stock rules for a specific SKU
     */
    @Modifying
    @Transactional
    @Query("UPDATE InfrastructureJpaEntitySafetyStockRule r SET r.isActive = false, r.updatedAt = CURRENT_TIMESTAMP WHERE r.skuId = :skuId")
    void deactivateAllBySkuId(@Param("skuId") String skuId);

    /**
     * Finds all rules for a specific SKU across all locations
     */
    List<InfrastructureJpaEntitySafetyStockRule> findAllBySkuId(String skuId);

    /**
     * Finds all active rules for a specific location
     */
    List<InfrastructureJpaEntitySafetyStockRule> findAllByLocationIdAndIsActive(String locationId, Boolean isActive);

    /**
     * Counts active rules for monitoring purposes
     */
    @Query("SELECT COUNT(r) FROM InfrastructureJpaEntitySafetyStockRule r WHERE r.isActive = true")
    long countActiveRules();

    /**
     * Checks if a rule exists for specific SKU and location
     */
    boolean existsBySkuIdAndLocationId(String skuId, String locationId);
}
