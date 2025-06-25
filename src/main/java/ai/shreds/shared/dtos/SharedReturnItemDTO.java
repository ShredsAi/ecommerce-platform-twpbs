package ai.shreds.shared.dtos;

import ai.shreds.shared.value_objects.SharedMoneyValue;

/**
 * DTO for individual return item data transfer.
 */
public class SharedReturnItemDTO {
    
    private String orderItemId;
    private String productId;
    private Integer quantity;
    private String returnReason;
    private String condition;
    private SharedMoneyValue refundAmount;
    
    // Default constructor
    public SharedReturnItemDTO() {}
    
    // All-args constructor
    public SharedReturnItemDTO(String orderItemId, String productId, Integer quantity,
                              String returnReason, String condition, SharedMoneyValue refundAmount) {
        this.orderItemId = orderItemId;
        this.productId = productId;
        this.quantity = quantity;
        this.returnReason = returnReason;
        this.condition = condition;
        this.refundAmount = refundAmount;
    }
    
    // Getters and Setters
    public String getOrderItemId() {
        return orderItemId;
    }
    
    public void setOrderItemId(String orderItemId) {
        this.orderItemId = orderItemId;
    }
    
    public String getProductId() {
        return productId;
    }
    
    public void setProductId(String productId) {
        this.productId = productId;
    }
    
    public Integer getQuantity() {
        return quantity;
    }
    
    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }
    
    public String getReturnReason() {
        return returnReason;
    }
    
    public void setReturnReason(String returnReason) {
        this.returnReason = returnReason;
    }
    
    public String getCondition() {
        return condition;
    }
    
    public void setCondition(String condition) {
        this.condition = condition;
    }
    
    public SharedMoneyValue getRefundAmount() {
        return refundAmount;
    }
    
    public void setRefundAmount(SharedMoneyValue refundAmount) {
        this.refundAmount = refundAmount;
    }
}
