package ai.shreds.shared.dtos;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

import ai.shreds.shared.value_objects.SharedMoneyValue;
import ai.shreds.application.dtos.ApplicationPaymentWebhookDTO;

/**
 * DTO representing a payment webhook request from payment provider.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SharedPaymentWebhookRequestDTO {
    
    @NotBlank(message = "Transaction ID is required")
    private String transactionId;
    
    @NotNull(message = "Order ID is required")
    private UUID orderId;
    
    @NotBlank(message = "Status is required")
    private String status;
    
    private String authCode;
    
    @NotNull(message = "Amount is required")
    private SharedMoneyValue amount;
    
    /**
     * Converts this shared DTO to application layer DTO.
     * 
     * @return ApplicationPaymentWebhookDTO with the same data
     */
    public ApplicationPaymentWebhookDTO toApplicationDTO() {
        return ApplicationPaymentWebhookDTO.fromSharedDTO(this);
    }
    
    /**
     * Validates the webhook request data.
     * 
     * @throws IllegalArgumentException if validation fails
     */
    public void validate() {
        if (transactionId == null || transactionId.trim().isEmpty()) {
            throw new IllegalArgumentException("Transaction ID is required");
        }
        if (orderId == null) {
            throw new IllegalArgumentException("Order ID is required");
        }
        if (status == null || status.trim().isEmpty()) {
            throw new IllegalArgumentException("Status is required");
        }
        if (amount == null) {
            throw new IllegalArgumentException("Amount is required");
        }
    }
    
    /**
     * Checks if the payment status indicates success.
     * 
     * @return true if status indicates successful payment
     */
    public boolean isSuccessfulPayment() {
        return "AUTHORIZED".equalsIgnoreCase(status) || 
               "CAPTURED".equalsIgnoreCase(status);
    }
    
    /**
     * Checks if the payment status indicates failure.
     * 
     * @return true if status indicates failed payment
     */
    public boolean isFailedPayment() {
        return "FAILED".equalsIgnoreCase(status) || 
               "DECLINED".equalsIgnoreCase(status);
    }
}