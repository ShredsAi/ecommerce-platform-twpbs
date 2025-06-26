package ai.shreds.shared.dtos;

import ai.shreds.shared.value_objects.SharedMoneyValue;

/**
 * DTO for order item data transfer.
 */
public class SharedOrderItemDTO {
    
    private String orderItemId;
    private String productId;
    private String productName;
    private Integer quantity;
    private SharedMoneyValue unitPrice;
    private SharedMoneyValue totalPrice;
    private Boolean isReturnable;
    
    // Default constructor
    public SharedOrderItemDTO() {}
    
    // All-args constructor
    public SharedOrderItemDTO(String orderItemId, String productId, String productName,
                             Integer quantity, SharedMoneyValue unitPrice,
                             SharedMoneyValue totalPrice, Boolean isReturnable) {
        this.orderItemId = orderItemId;
        this.productId = productId;
        this.productName = productName;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.totalPrice = totalPrice;
        this.isReturnable = isReturnable;
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
    
    public String getProductName() {
        return productName;
    }
    
    public void setProductName(String productName) {
        this.productName = productName;
    }
    
    public Integer getQuantity() {
        return quantity;
    }
    
    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }
    
    public SharedMoneyValue getUnitPrice() {
        return unitPrice;
    }
    
    public void setUnitPrice(SharedMoneyValue unitPrice) {
        this.unitPrice = unitPrice;
    }
    
    public SharedMoneyValue getTotalPrice() {
        return totalPrice;
    }
    
    public void setTotalPrice(SharedMoneyValue totalPrice) {
        this.totalPrice = totalPrice;
    }
    
    public Boolean getIsReturnable() {
        return isReturnable;
    }
    
    public void setIsReturnable(Boolean isReturnable) {
        this.isReturnable = isReturnable;
    }
}
