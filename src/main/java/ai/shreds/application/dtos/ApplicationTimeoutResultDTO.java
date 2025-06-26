package ai.shreds.application.dtos;

import ai.shreds.shared.dtos.SharedTimeoutDetailDTO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO summarizing the outcome of timeout processing.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApplicationTimeoutResultDTO {
    private int processedCount;
    private int failedCount;
    private List<SharedTimeoutDetailDTO> details;
    private long executionTime;
}
