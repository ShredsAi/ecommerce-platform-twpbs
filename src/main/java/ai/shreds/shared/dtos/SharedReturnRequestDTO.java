package ai.shreds.shared.dtos;

import ai.shreds.shared.enums.SharedReturnReasonEnum;
import ai.shreds.shared.enums.SharedReturnStatusEnum;
import ai.shreds.shared.value_objects.SharedMoneyValue;
import ai.shreds.shared.value_objects.SharedAddressValue;
import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO for return request data transfer.
 * Includes conversion methods to/from domain objects.
 */
public class SharedReturnRequestDTO {
    
    private String returnId;
    private String orderId;
    private String customerId;
    private String rmaNumber;
    private SharedReturnReasonEnum reason;
    private SharedReturnStatusEnum status;
    private List<SharedReturnItemDTO> items;
    private LocalDateTime requestedAt;
    private LocalDateTime receivedAt;
    private LocalDateTime refundedAt;
    private SharedMoneyValue refundAmount;
    private SharedAddressValue returnAddress;
    private String instructions;
    
    // Default constructor
    public SharedReturnRequestDTO() {}
    
    // All-args constructor
    public SharedReturnRequestDTO(String returnId, String orderId, String customerId, String rmaNumber,
                                 SharedReturnReasonEnum reason, SharedReturnStatusEnum status,
                                 List<SharedReturnItemDTO> items, LocalDateTime requestedAt,
                                 LocalDateTime receivedAt, LocalDateTime refundedAt,
                                 SharedMoneyValue refundAmount, SharedAddressValue returnAddress,
                                 String instructions) {
        this.returnId = returnId;
        this.orderId = orderId;
        this.customerId = customerId;
        this.rmaNumber = rmaNumber;
        this.reason = reason;
        this.status = status;
        this.items = items;
        this.requestedAt = requestedAt;
        this.receivedAt = receivedAt;
        this.refundedAt = refundedAt;
        this.refundAmount = refundAmount;
        this.returnAddress = returnAddress;
        this.instructions = instructions;
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
    
    public String getCustomerId() {
        return customerId;
    }
    
    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }
    
    public String getRmaNumber() {
        return rmaNumber;
    }
    
    public void setRmaNumber(String rmaNumber) {
        this.rmaNumber = rmaNumber;
    }
    
    public SharedReturnReasonEnum getReason() {
        return reason;
    }
    
    public void setReason(SharedReturnReasonEnum reason) {
        this.reason = reason;
    }
    
    public SharedReturnStatusEnum getStatus() {
        return status;
    }
    
    public void setStatus(SharedReturnStatusEnum status) {
        this.status = status;
    }
    
    public List<SharedReturnItemDTO> getItems() {
        return items;
    }
    
    public void setItems(List<SharedReturnItemDTO> items) {
        this.items = items;
    }
    
    public LocalDateTime getRequestedAt() {
        return requestedAt;
    }
    
    public void setRequestedAt(LocalDateTime requestedAt) {
        this.requestedAt = requestedAt;
    }
    
    public LocalDateTime getReceivedAt() {
        return receivedAt;
    }
    
    public void setReceivedAt(LocalDateTime receivedAt) {
        this.receivedAt = receivedAt;
    }
    
    public LocalDateTime getRefundedAt() {
        return refundedAt;
    }
    
    public void setRefundedAt(LocalDateTime refundedAt) {
        this.refundedAt = refundedAt;
    }
    
    public SharedMoneyValue getRefundAmount() {
        return refundAmount;
    }
    
    public void setRefundAmount(SharedMoneyValue refundAmount) {
        this.refundAmount = refundAmount;
    }
    
    public SharedAddressValue getReturnAddress() {
        return returnAddress;
    }
    
    public void setReturnAddress(SharedAddressValue returnAddress) {
        this.returnAddress = returnAddress;
    }
    
    public String getInstructions() {
        return instructions;
    }
    
    public void setInstructions(String instructions) {
        this.instructions = instructions;
    }
    
    /**
     * Converts this DTO to domain object.
     * Implementation depends on the domain entity structure.
     */
    public Object toDomain() {
        // This method will be implemented when domain layer is created
        // For now, returning null as placeholder
        return null;
    }
    
    /**
     * Creates DTO from domain object.
     * Implementation depends on the domain entity structure.
     */
    public static SharedReturnRequestDTO fromDomain(Object domain) {
        // This method will be implemented when domain layer is created
        // For now, returning null as placeholder
        return null;
    }
}
