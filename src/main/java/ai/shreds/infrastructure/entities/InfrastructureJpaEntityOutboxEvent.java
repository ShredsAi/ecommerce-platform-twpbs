package ai.shreds.infrastructure.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Column;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Index;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.ToString;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
    name = "outbox_event",
    indexes = {
        @Index(name = "idx_outbox_event_processed", columnList = "processed,occurred_on"),
        @Index(name = "idx_outbox_event_aggregate", columnList = "aggregate_id,aggregate_type"),
        @Index(name = "idx_outbox_event_occurred", columnList = "occurred_on")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"payload"})
public class InfrastructureJpaEntityOutboxEvent {

    @Id
    @Column(name = "event_id", nullable = false, updatable = false)
    private UUID eventId;

    @NotNull
    @Column(name = "aggregate_id", nullable = false)
    private UUID aggregateId;

    @NotNull
    @Size(min = 1, max = 64)
    @Column(name = "aggregate_type", nullable = false, length = 64)
    private String aggregateType;

    @NotNull
    @Size(min = 1, max = 128)
    @Column(name = "event_type", nullable = false, length = 128)
    private String eventType;

    @NotNull
    @Column(name = "payload", nullable = false)
    @JdbcTypeCode(SqlTypes.JSON) // This works with both PostgreSQL and H2
    private String payload;

    @Column(name = "occurred_on", nullable = false, updatable = false)
    private Instant occurredOn;

    @Column(name = "processed", nullable = false)
    private boolean processed = false;

    @Column(name = "processed_on")
    private Instant processedOn;

    @PrePersist
    public void prePersist() {
        if (eventId == null) {
            eventId = UUID.randomUUID();
        }
        if (occurredOn == null) {
            occurredOn = Instant.now();
        }
    }
    
    // Business method to mark as processed
    public void markAsProcessed() {
        this.processed = true;
        this.processedOn = Instant.now();
    }
    
    // Validation method to ensure required fields are set
    public void validateEvent() {
        if (aggregateId == null) {
            throw new IllegalArgumentException("Aggregate ID cannot be null");
        }
        if (aggregateType == null || aggregateType.trim().isEmpty()) {
            throw new IllegalArgumentException("Aggregate type cannot be null or empty");
        }
        if (eventType == null || eventType.trim().isEmpty()) {
            throw new IllegalArgumentException("Event type cannot be null or empty");
        }
        if (payload == null || payload.trim().isEmpty()) {
            throw new IllegalArgumentException("Payload cannot be null or empty");
        }
    }

    // Additional getter for MapStruct
    public boolean getProcessed() {
        return this.processed;
    }
}