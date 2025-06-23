package ai.shreds.application.dtos;

import ai.shreds.shared.enums.SharedEnumCorrelationStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO representing the outcome of webhook-to-payment correlation.
 */
@Data
@Builder
public class ApplicationCorrelationResultDTO {
    private SharedEnumCorrelationStatus correlationStatus;
    private UUID paymentId;
    private LocalDateTime correlatedAt;
    private String failureReason;
}