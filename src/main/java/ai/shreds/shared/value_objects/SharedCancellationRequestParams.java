package ai.shreds.shared.value_objects;

/**
 * Value object representing cancellation request parameters.
 */
public class SharedCancellationRequestParams {
    private String orderId;
    private String cancellationReason;
    private Boolean refundRequired;

    public SharedCancellationRequestParams() {}

    public SharedCancellationRequestParams(String orderId, String cancellationReason, Boolean refundRequired) {
        this.orderId = orderId;
        this.cancellationReason = cancellationReason;
        this.refundRequired = refundRequired;
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public String getCancellationReason() {
        return cancellationReason;
    }

    public void setCancellationReason(String cancellationReason) {
        this.cancellationReason = cancellationReason;
    }

    public Boolean getRefundRequired() {
        return refundRequired;
    }

    public void setRefundRequired(Boolean refundRequired) {
        this.refundRequired = refundRequired;
    }
}