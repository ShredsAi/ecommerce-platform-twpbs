package ai.shreds.infrastructure.repositories;

import lombok.*;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "saga_state")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InfrastructureSagaStateJPAEntity {

    @Id
    @Column(name = "saga_id", nullable = false, updatable = false)
    private UUID sagaId;

    @Column(name = "order_id", nullable = false)
    private UUID orderId;

    @Column(name = "current_step", nullable = false)
    private String currentStep;

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "retry_count", nullable = false)
    private Integer retryCount;

    @Column(name = "last_activity", nullable = false)
    private Instant lastActivity;

    @Column(name = "timeout_threshold", nullable = false)
    private Long timeoutThreshold;

    @Column(name = "next_retry", nullable = false)
    private Instant nextRetry;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private Integer version;
}