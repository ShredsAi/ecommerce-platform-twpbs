package ai.shreds.domain.entities;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Represents a status update entry for a payment.
 */
@Entity
@Table(name = "payment_status_updates")
public class DomainEntityPaymentStatusUpdate {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;
    
    @Column(name = "payment_id", nullable = false)
    private UUID paymentId;
    
    @Column(name = "old_status")
    private String oldStatus;
    
    @Column(name = "new_status")
    private String newStatus;
    
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    @Column(name = "processed", nullable = false)
    private boolean processed;

    // Default constructor for JPA
    protected DomainEntityPaymentStatusUpdate() {}

    public DomainEntityPaymentStatusUpdate(Long id,
                                           UUID paymentId,
                                           String oldStatus,
                                           String newStatus,
                                           LocalDateTime updatedAt,
                                           boolean processed) {
        this.id = id;
        this.paymentId = paymentId;
        this.oldStatus = oldStatus;
        this.newStatus = newStatus;
        this.updatedAt = updatedAt;
        this.processed = processed;
    }

    public Long getId() {
        return id;
    }

    public UUID getPaymentId() {
        return paymentId;
    }

    public String getOldStatus() {
        return oldStatus;
    }

    public String getNewStatus() {
        return newStatus;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public boolean isProcessed() {
        return processed;
    }

    /**
     * Marks this status update as processed.
     */
    public void markAsProcessed() {
        this.processed = true;
    }
}