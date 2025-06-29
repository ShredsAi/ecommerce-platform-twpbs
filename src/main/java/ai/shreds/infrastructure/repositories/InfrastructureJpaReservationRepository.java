package ai.shreds.infrastructure.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import ai.shreds.infrastructure.entities.InfrastructureJpaEntityReservation;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface InfrastructureJpaReservationRepository extends JpaRepository<InfrastructureJpaEntityReservation, UUID> {

    /**
     * Finds reservations by SKU ID, location ID, and statuses
     */
    List<InfrastructureJpaEntityReservation> findBySkuIdAndLocationIdAndStatusIn(
        String skuId, String locationId, List<String> statuses);
    
    /**
     * Finds all reservations by status
     */
    List<InfrastructureJpaEntityReservation> findByStatus(String status);
    
    /**
     * Finds all expired reservations (expiration time in the past but status still PENDING or CONFIRMED)
     */
    @Query("SELECT r FROM InfrastructureJpaEntityReservation r WHERE r.expiresAt < :now AND r.status IN ('PENDING', 'CONFIRMED')")
    List<InfrastructureJpaEntityReservation> findExpiredReservations(@Param("now") Instant now);
    
    /**
     * Finds all reservations for a specific SKU
     */
    List<InfrastructureJpaEntityReservation> findBySkuId(String skuId);
    
    /**
     * Finds all reservations for a specific location
     */
    List<InfrastructureJpaEntityReservation> findByLocationId(String locationId);
    
    /**
     * Counts active reservations (PENDING or CONFIRMED) for a specific SKU and location
     */
    @Query("SELECT COUNT(r) FROM InfrastructureJpaEntityReservation r WHERE r.skuId = :skuId AND r.locationId = :locationId AND r.status IN ('PENDING', 'CONFIRMED')")
    long countActiveReservations(@Param("skuId") String skuId, @Param("locationId") String locationId);
    
    /**
     * Calculates the total reserved quantity for a specific SKU and location
     */
    @Query("SELECT SUM(r.quantity) FROM InfrastructureJpaEntityReservation r WHERE r.skuId = :skuId AND r.locationId = :locationId AND r.status IN ('PENDING', 'CONFIRMED')")
    java.math.BigDecimal calculateTotalReservedQuantity(@Param("skuId") String skuId, @Param("locationId") String locationId);
}