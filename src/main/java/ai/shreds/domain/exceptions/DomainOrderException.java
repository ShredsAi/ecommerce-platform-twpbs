package ai.shreds.domain.exceptions;

import lombok.Getter;

@Getter
public class DomainOrderException extends RuntimeException {
    private final String errorCode;
    private final String orderId;

    public DomainOrderException(String message, String errorCode, String orderId) {
        super(message);
        this.errorCode = errorCode;
        this.orderId = orderId;
    }

    public DomainOrderException(String message, String errorCode, String orderId, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.orderId = orderId;
    }

    @Override
    public String getMessage() {
        StringBuilder sb = new StringBuilder(super.getMessage());
        if (errorCode != null) {
            sb.append(" [Error Code: ").append(errorCode).append("]");
        }
        if (orderId != null) {
            sb.append(" [Order ID: ").append(orderId).append("]");
        }
        return sb.toString();
    }
}