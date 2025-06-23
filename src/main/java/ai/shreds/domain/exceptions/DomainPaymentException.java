package ai.shreds.domain.exceptions;

/**
 * Base exception for payment-related domain errors.
 * This exception serves as the parent class for all domain-specific payment exceptions.
 */
public class DomainPaymentException extends RuntimeException {
    private final String code;

    public DomainPaymentException(String message) {
        super(message);
        this.code = "PAYMENT_ERROR";
    }

    public DomainPaymentException(String message, String code) {
        super(message);
        this.code = code != null ? code : "PAYMENT_ERROR";
    }

    public DomainPaymentException(String message, Throwable cause) {
        super(message, cause);
        this.code = "PAYMENT_ERROR";
    }

    public DomainPaymentException(String message, String code, Throwable cause) {
        super(message, cause);
        this.code = code != null ? code : "PAYMENT_ERROR";
    }

    /**
     * Gets the error code associated with this exception.
     * 
     * @return the error code
     */
    public String getCode() {
        return code;
    }

    /**
     * Creates a payment exception for invalid amount scenarios.
     * 
     * @param amount the invalid amount
     * @return a new DomainPaymentException
     */
    public static DomainPaymentException invalidAmount(String amount) {
        return new DomainPaymentException(
            "Invalid payment amount: " + amount, 
            "INVALID_AMOUNT"
        );
    }

    /**
     * Creates a payment exception for unsupported currency scenarios.
     * 
     * @param currency the unsupported currency
     * @return a new DomainPaymentException
     */
    public static DomainPaymentException unsupportedCurrency(String currency) {
        return new DomainPaymentException(
            "Unsupported currency: " + currency, 
            "UNSUPPORTED_CURRENCY"
        );
    }

    /**
     * Creates a payment exception for processor unavailable scenarios.
     * 
     * @param processorType the unavailable processor
     * @return a new DomainPaymentException
     */
    public static DomainPaymentException processorUnavailable(String processorType) {
        return new DomainPaymentException(
            "Payment processor unavailable: " + processorType, 
            "PROCESSOR_UNAVAILABLE"
        );
    }

    /**
     * Creates a payment exception for payment method not found scenarios.
     * 
     * @param paymentMethodId the payment method ID
     * @return a new DomainPaymentException
     */
    public static DomainPaymentException paymentMethodNotFound(String paymentMethodId) {
        return new DomainPaymentException(
            "Payment method not found: " + paymentMethodId, 
            "PAYMENT_METHOD_NOT_FOUND"
        );
    }

    /**
     * Creates a payment exception for insufficient funds scenarios.
     * 
     * @return a new DomainPaymentException
     */
    public static DomainPaymentException insufficientFunds() {
        return new DomainPaymentException(
            "Insufficient funds for payment", 
            "INSUFFICIENT_FUNDS"
        );
    }

    /**
     * Creates a payment exception for declined payment scenarios.
     * 
     * @param reason the decline reason
     * @return a new DomainPaymentException
     */
    public static DomainPaymentException paymentDeclined(String reason) {
        return new DomainPaymentException(
            "Payment declined: " + reason, 
            "PAYMENT_DECLINED"
        );
    }

    @Override
    public String toString() {
        return "DomainPaymentException{" +
                "code='" + code + '\'' +
                ", message='" + getMessage() + '\'' +
                '}';
    }
}