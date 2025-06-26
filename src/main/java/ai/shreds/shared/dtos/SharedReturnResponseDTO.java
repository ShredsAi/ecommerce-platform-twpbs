package ai.shreds.shared.dtos;

import ai.shreds.shared.enums.SharedReturnStatusEnum;
import java.time.Instant;

public class SharedReturnResponseDTO {
    private String returnId;
    private String orderId;
    private SharedReturnStatusEnum status;
    private Instant requestedAt;

    public SharedReturnResponseDTO() {
    }

    public SharedReturnResponseDTO(String returnId, String orderId, SharedReturnStatusEnum status, Instant requestedAt) {
        this.returnId = returnId;
        this.orderId = orderId;
        this.status = status;
        this.requestedAt = requestedAt;
    }

    public String getReturnId() {
        return returnId;
    }

    public void setReturnId(String returnId) {
        this.returnId = returnId;
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public SharedReturnStatusEnum getStatus() {
        return status;
    }

    public void setStatus(SharedReturnStatusEnum status) {
        this.status = status;
    }

    public Instant getRequestedAt() {
        return requestedAt;
    }

    public void setRequestedAt(Instant requestedAt) {
        this.requestedAt = requestedAt;
    }
}