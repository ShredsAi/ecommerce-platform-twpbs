package ai.shreds.infrastructure.repositories;

import lombok.*;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "order_events")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InfrastructureOrderEventJPAEntity {

    @Id
    @Column(name = "event_id", nullable = false, updatable = false)
    private UUID eventId;

    @Column(name = "order_id", nullable = false)
    private UUID orderId;

    @Column(name = "event_type", nullable = false)
    private String eventType;

    @Column(name = "event_timestamp", nullable = false)
    private Instant eventTimestamp;

    @Column(name = "event_data", columnDefinition = "TEXT", nullable = false)
    private String eventData;

    @Column(name = "previous_status", nullable = false)
    private String previousStatus;

    @Column(name = "new_status", nullable = false)
    private String newStatus;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}