package ai.shreds.infrastructure.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Index;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.ToString;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
    name = "erp_reconciliation",
    indexes = {
        @Index(name = "idx_erp_reconciliation_batch", columnList = "batch_id", unique = true),
        @Index(name = "idx_erp_reconciliation_status", columnList = "status"),
        @Index(name = "idx_erp_reconciliation_processed", columnList = "processed_at")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"errors"})
public class InfrastructureJpaEntityERPReconciliation {

    @Id
    @Column(name = "reconciliation_id", nullable = false, updatable = false)
    private UUID reconciliationId;

    @NotNull
    @Size(min = 1, max = 64)
    @Column(name = "batch_id", nullable = false, unique = true, length = 64)
    private String batchId;

    @NotNull
    @Size(min = 1, max = 32)
    @Column(name = "status", nullable = false, length = 32)
    private String status = "PENDING";

    @Column(name = "processed_at")
    private Instant processedAt;

    @Min(0)
    @Column(name = "total_records", nullable = false)
    private int totalRecords = 0;

    @Min(0)
    @Column(name = "success_count", nullable = false)
    private int successCount = 0;

    @Min(0)
    @Column(name = "error_count", nullable = false)
    private int errorCount = 0;

    @Column(name = "errors", columnDefinition = "TEXT")
    private String errors;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    public void prePersist() {
        Instant now = Instant.now();
        if (reconciliationId == null) {
            reconciliationId = UUID.randomUUID();
        }
        if (createdAt == null) {
            createdAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
        if (status == null || status.trim().isEmpty()) {
            status = "PENDING";
        }
    }
    
    @PreUpdate
    public void preUpdate() {
        updatedAt = Instant.now();
    }
    
    // Business methods for status management
    public void start() {
        this.status = "IN_PROGRESS";
        this.processedAt = Instant.now();
    }
    
    public void complete() {
        this.status = "COMPLETED";
        this.processedAt = Instant.now();
    }
    
    public void fail(String reason) {
        this.status = "FAILED";
        this.processedAt = Instant.now();
        if (this.errors == null || this.errors.trim().isEmpty()) {
            this.errors = reason;
        } else {
            this.errors += "; " + reason;
        }
    }
    
    public void recordSuccess() {
        this.successCount++;
    }
    
    public void recordError() {
        this.errorCount++;
    }
    
    // Validation method
    public void validateReconciliation() {
        if (totalRecords < 0) {
            throw new IllegalArgumentException("Total records cannot be negative");
        }
        if (successCount < 0) {
            throw new IllegalArgumentException("Success count cannot be negative");
        }
        if (errorCount < 0) {
            throw new IllegalArgumentException("Error count cannot be negative");
        }
        if (successCount + errorCount > totalRecords) {
            throw new IllegalArgumentException("Sum of success and error counts cannot exceed total records");
        }
    }
}
