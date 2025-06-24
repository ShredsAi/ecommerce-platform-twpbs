package ai.shreds.application.exceptions;

import ai.shreds.shared.enums.SharedErrorTypeEnum;

public class ApplicationOrderCreationException extends RuntimeException {

    private final SharedErrorTypeEnum errorType;
    private final String cartId;

    public ApplicationOrderCreationException(String message, Throwable cause, SharedErrorTypeEnum errorType, String cartId) {
        super(message, cause);
        this.errorType = errorType;
        this.cartId = cartId;
    }

    public SharedErrorTypeEnum getErrorType() {
        return errorType;
    }

    public String getCartId() {
        return cartId;
    }
}
