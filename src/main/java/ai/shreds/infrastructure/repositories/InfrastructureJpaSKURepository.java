package ai.shreds.infrastructure.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import ai.shreds.infrastructure.entities.InfrastructureJpaEntitySKU;

import java.util.List;

@Repository
public interface InfrastructureJpaSKURepository extends JpaRepository<InfrastructureJpaEntitySKU, String> {

    /**
     * Checks if a SKU exists and is active
     */
    boolean existsBySkuIdAndIsActive(String skuId, boolean isActive);
    
    /**
     * Finds all SKUs by product ID
     */
    List<InfrastructureJpaEntitySKU> findByProductId(String productId);
    
    /**
     * Finds all active SKUs
     */
    List<InfrastructureJpaEntitySKU> findByIsActiveTrue();
    
    /**
     * Finds SKU by vendor SKU
     */
    @Query("SELECT s FROM InfrastructureJpaEntitySKU s WHERE s.vendorSku = :vendorSku")
    List<InfrastructureJpaEntitySKU> findByVendorSku(@Param("vendorSku") String vendorSku);
    
    /**
     * Counts all active SKUs
     */
    long countByIsActive(boolean isActive);
}