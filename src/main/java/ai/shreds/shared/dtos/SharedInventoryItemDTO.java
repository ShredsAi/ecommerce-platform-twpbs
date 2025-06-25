package ai.shreds.shared.dtos;

/**
 * DTO for inventory item data transfer.
 */
public class SharedInventoryItemDTO {
    
    private String productId;
    private Integer quantity;
    private String action;
    private String warehouseId;
    
    // Default constructor
    public SharedInventoryItemDTO() {}
    
    // All-args constructor
    public SharedInventoryItemDTO(String productId, Integer quantity, String action,
                                 String warehouseId) {
        this.productId = productId;
        this.quantity = quantity;
        this.action = action;
        this.warehouseId = warehouseId;
    }
    
    // Getters and Setters
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
    
    public String getAction() {
        return action;
    }
    
    public void setAction(String action) {
        this.action = action;
    }
    
    public String getWarehouseId() {
        return warehouseId;
    }
    
    public void setWarehouseId(String warehouseId) {
        this.warehouseId = warehouseId;
    }
}
