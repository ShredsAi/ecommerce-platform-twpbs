package ai.shreds.infrastructure.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import ai.shreds.infrastructure.entities.InfrastructureJpaEntityLocation;

import java.util.List;

@Repository
public interface InfrastructureJpaLocationRepository extends JpaRepository<InfrastructureJpaEntityLocation, String> {

    /**
     * Checks if a location exists and is active
     */
    boolean existsByLocationIdAndIsActive(String locationId, boolean isActive);
    
    /**
     * Finds all locations by type
     */
    List<InfrastructureJpaEntityLocation> findByType(String type);
    
    /**
     * Finds all active locations
     */
    List<InfrastructureJpaEntityLocation> findByIsActiveTrue();
    
    /**
     * Finds locations by name (partial match)
     */
    @Query("SELECT l FROM InfrastructureJpaEntityLocation l WHERE LOWER(l.name) LIKE LOWER(CONCAT('%', :namePart, '%'))")
    List<InfrastructureJpaEntityLocation> findByNameContainingIgnoreCase(@Param("namePart") String namePart);
    
    /**
     * Counts all active locations
     */
    long countByIsActive(boolean isActive);
    
    /**
     * Counts locations by type
     */
    long countByType(String type);
}