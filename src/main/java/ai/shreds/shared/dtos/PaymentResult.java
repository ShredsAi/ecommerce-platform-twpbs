package ai.shreds.shared.dtos;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.io.Serializable;
import ai.shreds.shared.value_objects.SharedMoneyValue;

/**
 * DTO representing the result of a payment operation.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResult implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private Boolean success;
    private String transactionId;
    private String status;
    private String authorizationCode;
    private SharedMoneyValue amount;
    private String errorMessage;
    
    /**
     * Checks if the payment operation was successful.
     * 
     * @return true if the operation succeeded
     */
    public boolean isSuccess() {
        return Boolean.TRUE.equals(success);
    }
    
    /**
     * Checks if the payment operation failed.
     * 
     * @return true if the operation failed
     */
    public boolean isFailed() {
        return !isSuccess();
    }
    
    /**
     * Checks if an error message is present.
     * 
     * @return true if error message is present and not empty
     */
    public boolean hasErrorMessage() {
        return errorMessage != null && !errorMessage.trim().isEmpty();
    }
    
    /**
     * Factory method to create a successful payment result.
     * 
     * @param transactionId the transaction ID
     * @param status the payment status
     * @param amount the payment amount
     * @return a new successful PaymentResult
     */
    public static PaymentResult success(String transactionId, String status, SharedMoneyValue amount) {
        return PaymentResult.builder()
                .success(true)
                .transactionId(transactionId)
                .status(status)
                .amount(amount)
                .build();
    }
    
    /**
     * Factory method to create a successful authorization result.
     * 
     * @param transactionId the transaction ID
     * @param authorizationCode the authorization code
     * @param amount the authorized amount
     * @return a new successful authorization PaymentResult
     */
    public static PaymentResult authorized(String transactionId, String authorizationCode, SharedMoneyValue amount) {
        return PaymentResult.builder()
                .success(true)
                .transactionId(transactionId)
                .status("AUTHORIZED")
                .authorizationCode(authorizationCode)
                .amount(amount)
                .build();
    }
    
    /**
     * Factory method to create a successful capture result.
     * 
     * @param transactionId the transaction ID
     * @param amount the captured amount
     * @return a new successful capture PaymentResult
     */
    public static PaymentResult captured(String transactionId, SharedMoneyValue amount) {
        return PaymentResult.builder()
                .success(true)
                .transactionId(transactionId)
                .status("CAPTURED")
                .amount(amount)
                .build();
    }
    
    /**
     * Factory method to create a failed payment result.
     * 
     * @param errorMessage the error message
     * @return a new failed PaymentResult
     */
    public static PaymentResult failed(String errorMessage) {
        return PaymentResult.builder()
                .success(false)
                .status("FAILED")
                .errorMessage(errorMessage)
                .build();
    }
    
    /**
     * Factory method to create a failed payment result with transaction ID.
     * 
     * @param transactionId the transaction ID
     * @param errorMessage the error message
     * @return a new failed PaymentResult with transaction ID
     */
    public static PaymentResult failed(String transactionId, String errorMessage) {
        return PaymentResult.builder()
                .success(false)
                .transactionId(transactionId)
                .status("FAILED")
                .errorMessage(errorMessage)
                .build();
    }
}