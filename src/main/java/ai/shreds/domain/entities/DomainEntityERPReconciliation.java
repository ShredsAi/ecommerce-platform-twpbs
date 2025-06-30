package ai.shreds.domain.entities;

import ai.shreds.domain.exceptions.DomainExceptionInvalidState;
import ai.shreds.domain.value_objects.DomainValueBatchId;
import ai.shreds.domain.value_objects.DomainValueReconciliationError;
import ai.shreds.domain.value_objects.DomainValueReconciliationId;
import ai.shreds.domain.value_objects.DomainEnumReconciliationStatus;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DomainEntityERPReconciliation {
    private final DomainValueReconciliationId reconciliationId;
    private final DomainValueBatchId batchId;
    private DomainEnumReconciliationStatus status;
    private Instant processedAt;
    private final int totalRecords;
    private int successCount;
    private int errorCount;
    private final List<DomainValueReconciliationError> errors;

    private DomainEntityERPReconciliation(DomainValueReconciliationId reconciliationId,
                                          DomainValueBatchId batchId,
                                          DomainEnumReconciliationStatus status,
                                          Instant processedAt,
                                          int totalRecords,
                                          int successCount,
                                          int errorCount,
                                          List<DomainValueReconciliationError> errors) {
        this.reconciliationId = reconciliationId;
        this.batchId = batchId;
        this.status = status;
        this.processedAt = processedAt;
        this.totalRecords = totalRecords;
        this.successCount = successCount;
        this.errorCount = errorCount;
        this.errors = errors;
    }

    public static DomainEntityERPReconciliation create(String batchIdValue, int totalRecords) {
        DomainValueReconciliationId id = DomainValueReconciliationId.create();
        DomainValueBatchId batch = new DomainValueBatchId(batchIdValue);
        return new DomainEntityERPReconciliation(id, batch, DomainEnumReconciliationStatus.PENDING,
                Instant.now(), totalRecords, 0, 0, new ArrayList<>());
    }

    public static DomainEntityERPReconciliation reconstruct(DomainValueReconciliationId reconciliationId,
                                                           DomainValueBatchId batchId,
                                                           DomainEnumReconciliationStatus status,
                                                           Instant processedAt,
                                                           int totalRecords,
                                                           int successCount,
                                                           int errorCount,
                                                           List<DomainValueReconciliationError> errors) {
        return new DomainEntityERPReconciliation(reconciliationId, batchId, status, processedAt, totalRecords, 
                                               successCount, errorCount, new ArrayList<>(errors));
    }

    public void start() {
        if (status != DomainEnumReconciliationStatus.PENDING) {
            throw new DomainExceptionInvalidState(status.name(), "start");
        }
        status = DomainEnumReconciliationStatus.IN_PROGRESS;
        processedAt = Instant.now();
    }

    public void recordSuccess() {
        if (status != DomainEnumReconciliationStatus.IN_PROGRESS) {
            throw new DomainExceptionInvalidState(status.name(), "recordSuccess");
        }
        successCount++;
    }

    public void recordError(DomainValueReconciliationError error) {
        if (status != DomainEnumReconciliationStatus.IN_PROGRESS) {
            throw new DomainExceptionInvalidState(status.name(), "recordError");
        }
        errors.add(error);
        errorCount++;
    }

    public void complete() {
        if (status != DomainEnumReconciliationStatus.IN_PROGRESS) {
            throw new DomainExceptionInvalidState(status.name(), "complete");
        }
        if (successCount + errorCount != totalRecords) {
            throw new DomainExceptionInvalidState("IN_PROGRESS", "complete with mismatched counts");
        }
        status = DomainEnumReconciliationStatus.COMPLETED;
    }

    public void fail(String reason) {
        if (status == DomainEnumReconciliationStatus.COMPLETED) {
            throw new DomainExceptionInvalidState(status.name(), "fail");
        }
        status = DomainEnumReconciliationStatus.FAILED;
        errors.add(new DomainValueReconciliationError(batchId.getValue(), "", reason, "FAIL"));
    }

    public DomainValueReconciliationId getReconciliationId() {
        return reconciliationId;
    }

    public DomainValueBatchId getBatchId() {
        return batchId;
    }

    public DomainEnumReconciliationStatus getStatus() {
        return status;
    }

    public Instant getProcessedAt() {
        return processedAt;
    }

    public int getTotalRecords() {
        return totalRecords;
    }

    public int getSuccessCount() {
        return successCount;
    }

    public int getErrorCount() {
        return errorCount;
    }

    public List<DomainValueReconciliationError> getErrors() {
        return Collections.unmodifiableList(errors);
    }

    public boolean isComplete() {
        return successCount + errorCount >= totalRecords;
    }

    public double getSuccessRate() {
        if (totalRecords == 0) {
            return 0.0;
        }
        return (double) successCount / totalRecords;
    }
}
