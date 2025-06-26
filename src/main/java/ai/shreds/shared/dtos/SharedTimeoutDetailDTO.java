package ai.shreds.shared.dtos;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/**
 * DTO containing timeout handling details shared across services.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SharedTimeoutDetailDTO {
    
    @NotNull(message = "Saga ID is required")
    private UUID sagaId;
    
    @NotNull(message = "Order ID is required")
    private UUID orderId;
    
    @NotBlank(message = "Timeout type is required")
    private String timeoutType;
    
    @NotBlank(message = "Action taken is required")
    private String actionTaken;
    
    @NotNull(message = "Success flag is required")
    private Boolean success;
    
    private String errorMessage;
    
    /**
     * Validates the timeout detail data.
     * 
     * @throws IllegalArgumentException if validation fails
     */
    public void validate() {
        if (sagaId == null) {
            throw new IllegalArgumentException("Saga ID is required");
        }
        if (orderId == null) {
            throw new IllegalArgumentException("Order ID is required");
        }
        if (timeoutType == null || timeoutType.trim().isEmpty()) {
            throw new IllegalArgumentException("Timeout type is required");
        }
        if (actionTaken == null || actionTaken.trim().isEmpty()) {
            throw new IllegalArgumentException("Action taken is required");
        }
        if (success == null) {
            throw new IllegalArgumentException("Success flag is required");
        }
    }
    
    /**
     * Checks if the timeout handling was successful.
     * 
     * @return true if timeout handling succeeded
     */
    public boolean wasSuccessful() {
        return Boolean.TRUE.equals(success);
    }
    
    /**
     * Checks if there is an error message.
     * 
     * @return true if error message is present and not empty
     */
    public boolean hasError() {
        return errorMessage != null && !errorMessage.trim().isEmpty();
    }
    
    /**
     * Factory method to create a successful timeout detail.
     * 
     * @param sagaId the saga ID
     * @param orderId the order ID
     * @param timeoutType the type of timeout
     * @param actionTaken the action that was taken
     * @return SharedTimeoutDetailDTO representing successful timeout handling
     */
    public static SharedTimeoutDetailDTO success(UUID sagaId, UUID orderId, String timeoutType, String actionTaken) {
        return SharedTimeoutDetailDTO.builder()
                .sagaId(sagaId)
                .orderId(orderId)
                .timeoutType(timeoutType)
                .actionTaken(actionTaken)
                .success(true)
                .build();
    }
    
    /**
     * Factory method to create a failed timeout detail.
     * 
     * @param sagaId the saga ID
     * @param orderId the order ID
     * @param timeoutType the type of timeout
     * @param actionTaken the action that was attempted
     * @param errorMessage the error message
     * @return SharedTimeoutDetailDTO representing failed timeout handling
     */
    public static SharedTimeoutDetailDTO failure(UUID sagaId, UUID orderId, String timeoutType, String actionTaken, String errorMessage) {
        return SharedTimeoutDetailDTO.builder()
                .sagaId(sagaId)
                .orderId(orderId)
                .timeoutType(timeoutType)
                .actionTaken(actionTaken)
                .success(false)
                .errorMessage(errorMessage)
                .build();
    }
}