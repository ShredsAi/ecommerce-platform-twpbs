package ai.shreds.application.dtos;

import java.util.UUID;
import java.util.Map;

/**
 * Command DTO for updating payment status in the application layer
 */
public class ApplicationUpdateStatusCommand {

    private UUID paymentId;
    private String newStatus;
    private Map<String, Object> processorResponse;

    public ApplicationUpdateStatusCommand() {}

    public ApplicationUpdateStatusCommand(UUID paymentId, String newStatus, Map<String, Object> processorResponse) {
        this.paymentId = paymentId;
        this.newStatus = newStatus;
        this.processorResponse = processorResponse;
    }

    public UUID getPaymentId() {
        return paymentId;
    }

    public void setPaymentId(UUID paymentId) {
        this.paymentId = paymentId;
    }

    public String getNewStatus() {
        return newStatus;
    }

    public void setNewStatus(String newStatus) {
        this.newStatus = newStatus;
    }

    public Map<String, Object> getProcessorResponse() {
        return processorResponse;
    }

    public void setProcessorResponse(Map<String, Object> processorResponse) {
        this.processorResponse = processorResponse;
    }
}