package ai.shreds.infrastructure.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import ai.shreds.infrastructure.entities.InfrastructureJpaEntityOutboxEvent;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface InfrastructureJpaOutboxEventRepository extends JpaRepository<InfrastructureJpaEntityOutboxEvent, UUID> {

    /**
     * Finds top 100 unprocessed events ordered by occurred time
     */
    List<InfrastructureJpaEntityOutboxEvent> findTop100ByProcessedFalseOrderByOccurredOn();
    
    /**
     * Finds top N unprocessed events ordered by occurred time
     */
    @Query("SELECT e FROM InfrastructureJpaEntityOutboxEvent e WHERE e.processed = false ORDER BY e.occurredOn ASC")
    List<InfrastructureJpaEntityOutboxEvent> findTopByProcessedFalseOrderByOccurredOn(@Param("limit") int limit);
    
    /**
     * Finds all unprocessed events
     */
    List<InfrastructureJpaEntityOutboxEvent> findByProcessedFalseOrderByOccurredOnAsc();
    
    /**
     * Finds events by aggregate ID
     */
    List<InfrastructureJpaEntityOutboxEvent> findByAggregateIdOrderByOccurredOnAsc(UUID aggregateId);
    
    /**
     * Finds events by aggregate type
     */
    List<InfrastructureJpaEntityOutboxEvent> findByAggregateType(String aggregateType);
    
    /**
     * Finds events by event type
     */
    List<InfrastructureJpaEntityOutboxEvent> findByEventType(String eventType);
    
    /**
     * Finds events occurred after a specific time
     */
    @Query("SELECT e FROM InfrastructureJpaEntityOutboxEvent e WHERE e.occurredOn >= :since ORDER BY e.occurredOn DESC")
    List<InfrastructureJpaEntityOutboxEvent> findRecentEvents(@Param("since") Instant since);
    
    /**
     * Counts unprocessed events
     */
    @Query("SELECT COUNT(e) FROM InfrastructureJpaEntityOutboxEvent e WHERE e.processed = false")
    long countUnprocessedEvents();
    
    /**
     * Finds events that have been processed
     */
    List<InfrastructureJpaEntityOutboxEvent> findByProcessedTrueOrderByProcessedOnDesc();
    
    /**
     * Finds events that failed to process (remained unprocessed for too long)
     */
    @Query("SELECT e FROM InfrastructureJpaEntityOutboxEvent e WHERE e.processed = false AND e.occurredOn < :before")
    List<InfrastructureJpaEntityOutboxEvent> findStaleUnprocessedEvents(@Param("before") Instant before);
}