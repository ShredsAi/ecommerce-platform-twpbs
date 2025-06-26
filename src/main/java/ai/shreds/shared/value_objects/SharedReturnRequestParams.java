package ai.shreds.shared.value_objects;

import jakarta.validation.constraints.NotBlank;

public class SharedReturnRequestParams {
    @NotBlank
    private String orderId;
    private String reason;
    private Boolean refundRequired;

    public SharedReturnRequestParams() {
    }

    public SharedReturnRequestParams(String orderId, String reason, Boolean refundRequired) {
        this.orderId = orderId;
        this.reason = reason;
        this.refundRequired = refundRequired;
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public Boolean getRefundRequired() {
        return refundRequired;
    }

    public void setRefundRequired(Boolean refundRequired) {
        this.refundRequired = refundRequired;
    }
}