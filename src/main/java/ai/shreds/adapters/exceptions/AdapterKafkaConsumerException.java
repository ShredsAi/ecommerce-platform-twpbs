package ai.shreds.adapters.exceptions;

public class AdapterKafkaConsumerException extends RuntimeException {
    private final String errorCode;
    private final String cartId;

    public AdapterKafkaConsumerException(String message, Throwable cause, String errorCode, String cartId) {
        super(message, cause);
        this.errorCode = errorCode;
        this.cartId = cartId;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public String getCartId() {
        return cartId;
    }
}
