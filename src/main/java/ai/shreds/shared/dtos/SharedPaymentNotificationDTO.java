package ai.shreds.shared.dtos;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.Map;
import java.util.UUID;
import ai.shreds.application.dtos.ApplicationPaymentWebhookDTO;
import ai.shreds.shared.value_objects.SharedMoneyValue;
import java.math.BigDecimal;

/**
 * DTO representing a payment notification received through JMS.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SharedPaymentNotificationDTO {
    
    @NotNull(message = "Order ID is required")
    private UUID orderId;
    
    @NotBlank(message = "Transaction ID is required")
    private String transactionId;
    
    @NotBlank(message = "Status is required")
    private String status;
    
    private Map<String, Object> payload;
    
    /**
     * Converts this shared DTO to an application layer DTO.
     * Extracts amount and auth code from payload if available.
     * 
     * @return ApplicationPaymentWebhookDTO with converted data
     */
    public ApplicationPaymentWebhookDTO toApplicationDTO() {
        String authCode = extractFromPayload("authCode", String.class);
        SharedMoneyValue amount = extractAmountFromPayload();
        
        return new ApplicationPaymentWebhookDTO(
            this.transactionId,
            this.orderId,
            this.status,
            authCode,
            amount
        );
    }
    
    /**
     * Validates the payment notification data.
     * 
     * @throws IllegalArgumentException if validation fails
     */
    public void validate() {
        if (orderId == null) {
            throw new IllegalArgumentException("Order ID is required");
        }
        if (transactionId == null || transactionId.trim().isEmpty()) {
            throw new IllegalArgumentException("Transaction ID is required");
        }
        if (status == null || status.trim().isEmpty()) {
            throw new IllegalArgumentException("Status is required");
        }
    }
    
    /**
     * Checks if the payment notification indicates success.
     * 
     * @return true if status indicates successful payment
     */
    public boolean isSuccessfulPayment() {
        return "AUTHORIZED".equalsIgnoreCase(status) || 
               "CAPTURED".equalsIgnoreCase(status);
    }
    
    /**
     * Checks if the payment notification indicates failure.
     * 
     * @return true if status indicates failed payment
     */
    public boolean isFailedPayment() {
        return "FAILED".equalsIgnoreCase(status) || 
               "DECLINED".equalsIgnoreCase(status);
    }
    
    /**
     * Extracts a value from the payload map with type safety.
     * 
     * @param key the key to extract
     * @param type the expected type
     * @return the extracted value or null if not found or wrong type
     */
    @SuppressWarnings("unchecked")
    private <T> T extractFromPayload(String key, Class<T> type) {
        if (payload == null || !payload.containsKey(key)) {
            return null;
        }
        Object value = payload.get(key);
        if (type.isInstance(value)) {
            return (T) value;
        }
        return null;
    }
    
    /**
     * Extracts amount information from payload and converts to SharedMoneyValue.
     * 
     * @return SharedMoneyValue extracted from payload or null if not available
     */
    private SharedMoneyValue extractAmountFromPayload() {
        if (payload == null) {
            return null;
        }
        
        Object amountValue = payload.get("amount");
        String currency = extractFromPayload("currency", String.class);
        
        if (amountValue != null && currency != null) {
            BigDecimal amount = null;
            if (amountValue instanceof BigDecimal) {
                amount = (BigDecimal) amountValue;
            } else if (amountValue instanceof Number) {
                amount = BigDecimal.valueOf(((Number) amountValue).doubleValue());
            } else if (amountValue instanceof String) {
                try {
                    amount = new BigDecimal((String) amountValue);
                } catch (NumberFormatException e) {
                    // Invalid amount format, return null
                    return null;
                }
            }
            
            if (amount != null) {
                return SharedMoneyValue.of(amount, currency);
            }
        }
        
        return null;
    }
}