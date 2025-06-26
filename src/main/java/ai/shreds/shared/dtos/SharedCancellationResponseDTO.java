package ai.shreds.shared.dtos;

/**
 * DTO for cancellation responses.
 */
public class SharedCancellationResponseDTO {
    private String cancellationId;
    private String orderId;
    private String status;
    private String message;

    public SharedCancellationResponseDTO() {}

    public SharedCancellationResponseDTO(String cancellationId, String orderId, String status, String message) {
        this.cancellationId = cancellationId;
        this.orderId = orderId;
        this.status = status;
        this.message = message;
    }

    public String getCancellationId() {
        return cancellationId;
    }

    public void setCancellationId(String cancellationId) {
        this.cancellationId = cancellationId;
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}