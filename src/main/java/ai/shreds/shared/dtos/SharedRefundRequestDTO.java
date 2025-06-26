package ai.shreds.shared.dtos;

import ai.shreds.shared.value_objects.SharedMoneyValue;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;

/**
 * Data Transfer Object for refund operations.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SharedRefundRequestDTO {
    
    private String refundId;
    
    private String orderId;
    
    private String cancellationId;
    
    private String returnId;
    
    private SharedMoneyValue amount;
    
    private String reason;
    
    private String status;
    
    private Instant requestedAt;
    
    private Instant processedAt;
    
    private String errorMessage;
    
    /**
     * Validates the refund request data.
     * 
     * @throws IllegalArgumentException if validation fails
     */
    public void validate() {
        if (orderId == null || orderId.trim().isEmpty()) {
            throw new IllegalArgumentException("Order ID is required");
        }
        if (amount == null) {
            throw new IllegalArgumentException("Amount is required");
        }
    }
    
    /**
     * Checks if the refund status indicates success.
     * 
     * @return true if status indicates successful refund
     */
    public boolean isSuccessfulRefund() {
        return "PROCESSED".equalsIgnoreCase(status) || 
               "COMPLETED".equalsIgnoreCase(status);
    }
    
    /**
     * Checks if the refund status indicates failure.
     * 
     * @return true if status indicates failed refund
     */
    public boolean isFailedRefund() {
        return "FAILED".equalsIgnoreCase(status) || 
               "DECLINED".equalsIgnoreCase(status);
    }
}
