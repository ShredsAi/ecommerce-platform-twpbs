package ai.shreds.shared.dtos;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO for inventory update data transfer.
 * Includes conversion methods to/from domain objects.
 */
public class SharedInventoryUpdateDTO {
    
    private String updateId;
    private String orderId;
    private String updateType;
    private List<SharedInventoryItemDTO> items;
    private LocalDateTime timestamp;
    private String source;
    
    // Default constructor
    public SharedInventoryUpdateDTO() {}
    
    // All-args constructor
    public SharedInventoryUpdateDTO(String updateId, String orderId, String updateType,
                                   List<SharedInventoryItemDTO> items, LocalDateTime timestamp,
                                   String source) {
        this.updateId = updateId;
        this.orderId = orderId;
        this.updateType = updateType;
        this.items = items;
        this.timestamp = timestamp;
        this.source = source;
    }
    
    // Getters and Setters
    public String getUpdateId() {
        return updateId;
    }
    
    public void setUpdateId(String updateId) {
        this.updateId = updateId;
    }
    
    public String getOrderId() {
        return orderId;
    }
    
    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }
    
    public String getUpdateType() {
        return updateType;
    }
    
    public void setUpdateType(String updateType) {
        this.updateType = updateType;
    }
    
    public List<SharedInventoryItemDTO> getItems() {
        return items;
    }
    
    public void setItems(List<SharedInventoryItemDTO> items) {
        this.items = items;
    }
    
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
    
    public String getSource() {
        return source;
    }
    
    public void setSource(String source) {
        this.source = source;
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
    public static SharedInventoryUpdateDTO fromDomain(Object domain) {
        // This method will be implemented when domain layer is created
        // For now, returning null as placeholder
        return null;
    }
}
