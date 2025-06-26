package ai.shreds.shared.dtos;

import ai.shreds.shared.value_objects.SharedMoneyValue;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * Data Transfer Object for payment service requests.
 * Used for authorize, capture, and refund operations.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SharedPaymentRequestDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    @NotBlank(message = "Order ID is required")
    private String orderId;

    @NotNull(message = "Amount is required")
    @Valid
    private SharedMoneyValue amount;

    @NotBlank(message = "Transaction type is required")
    private String transactionType; // AUTHORIZE, CAPTURE, REFUND

    private String transactionId; // Required for CAPTURE and REFUND, optional for AUTHORIZE

    /**
     * Validates the payment request data.
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
        if (transactionType == null || transactionType.trim().isEmpty()) {
            throw new IllegalArgumentException("Transaction type is required");
        }

        // Validate transaction ID requirement based on type
        if (requiresTransactionId() && (transactionId == null || transactionId.trim().isEmpty())) {
            throw new IllegalArgumentException("Transaction ID is required for " + transactionType + " operations");
        }
    }

    /**
     * Checks if this is an authorization request.
     *
     * @return true if transaction type is AUTHORIZE
     */
    public boolean isAuthorizeRequest() {
        return "AUTHORIZE".equalsIgnoreCase(transactionType);
    }

    /**
     * Checks if this is a capture request.
     *
     * @return true if transaction type is CAPTURE
     */
    public boolean isCaptureRequest() {
        return "CAPTURE".equalsIgnoreCase(transactionType);
    }

    /**
     * Checks if this is a refund request.
     *
     * @return true if transaction type is REFUND
     */
    public boolean isRefundRequest() {
        return "REFUND".equalsIgnoreCase(transactionType);
    }

    /**
     * Checks if the transaction type requires a transaction ID.
     *
     * @return true if CAPTURE or REFUND operation
     */
    public boolean requiresTransactionId() {
        return isCaptureRequest() || isRefundRequest();
    }

    /**
     * Factory method to create an authorization request.
     *
     * @param orderId the order ID
     * @param amount the amount to authorize
     * @return a new authorization SharedPaymentRequestDTO
     */
    public static SharedPaymentRequestDTO createAuthorizeRequest(String orderId, SharedMoneyValue amount) {
        return SharedPaymentRequestDTO.builder()
                .orderId(orderId)
                .amount(amount)
                .transactionType("AUTHORIZE")
                .build();
    }

    /**
     * Factory method to create a capture request.
     *
     * @param orderId the order ID
     * @param amount the amount to capture
     * @param transactionId the transaction ID from authorization
     * @return a new capture SharedPaymentRequestDTO
     */
    public static SharedPaymentRequestDTO createCaptureRequest(String orderId, SharedMoneyValue amount, String transactionId) {
        return SharedPaymentRequestDTO.builder()
                .orderId(orderId)
                .amount(amount)
                .transactionType("CAPTURE")
                .transactionId(transactionId)
                .build();
    }

    /**
     * Factory method to create a refund request.
     *
     * @param orderId the order ID
     * @param amount the amount to refund
     * @param transactionId the transaction ID to refund
     * @return a new refund SharedPaymentRequestDTO
     */
    public static SharedPaymentRequestDTO createRefundRequest(String orderId, SharedMoneyValue amount, String transactionId) {
        return SharedPaymentRequestDTO.builder()
                .orderId(orderId)
                .amount(amount)
                .transactionType("REFUND")
                .transactionId(transactionId)
                .build();
    }
}