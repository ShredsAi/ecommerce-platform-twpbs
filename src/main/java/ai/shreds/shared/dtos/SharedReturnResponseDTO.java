package ai.shreds.shared.dtos;

import ai.shreds.shared.value_objects.SharedMoneyValue;
import ai.shreds.shared.value_objects.SharedAddressValue;
import java.time.LocalDateTime;

/**
 * DTO for return response data transfer.
 */
public class SharedReturnResponseDTO {
    
    private String returnId;
    private String orderId;
    private String rmaNumber;
    private String status;
    private LocalDateTime requestedAt;
    private String returnInstructions;
    private SharedAddressValue returnAddress;
    private SharedMoneyValue estimatedRefund;
    private Boolean success;
    private String message;
    
    // Default constructor
    public SharedReturnResponseDTO() {}
    
    // All-args constructor
    public SharedReturnResponseDTO(String returnId, String orderId, String rmaNumber,
                                  String status, LocalDateTime requestedAt, String returnInstructions,
                                  SharedAddressValue returnAddress, SharedMoneyValue estimatedRefund,
                                  Boolean success, String message) {
        this.returnId = returnId;
        this.orderId = orderId;
        this.rmaNumber = rmaNumber;
        this.status = status;
        this.requestedAt = requestedAt;
        this.returnInstructions = returnInstructions;
        this.returnAddress = returnAddress;
        this.estimatedRefund = estimatedRefund;
        this.success = success;
        this.message = message;
    }
    
    // Getters and Setters
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
    
    public String getRmaNumber() {
        return rmaNumber;
    }
    
    public void setRmaNumber(String rmaNumber) {
        this.rmaNumber = rmaNumber;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public LocalDateTime getRequestedAt() {
        return requestedAt;
    }
    
    public void setRequestedAt(LocalDateTime requestedAt) {
        this.requestedAt = requestedAt;
    }
    
    public String getReturnInstructions() {
        return returnInstructions;
    }
    
    public void setReturnInstructions(String returnInstructions) {
        this.returnInstructions = returnInstructions;
    }
    
    public SharedAddressValue getReturnAddress() {
        return returnAddress;
    }
    
    public void setReturnAddress(SharedAddressValue returnAddress) {
        this.returnAddress = returnAddress;
    }
    
    public SharedMoneyValue getEstimatedRefund() {
        return estimatedRefund;
    }
    
    public void setEstimatedRefund(SharedMoneyValue estimatedRefund) {
        this.estimatedRefund = estimatedRefund;
    }
    
    public Boolean getSuccess() {
        return success;
    }
    
    public void setSuccess(Boolean success) {
        this.success = success;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
}