package ai.shreds.shared.dtos;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotEmpty;
import java.io.Serializable;
import java.util.List;

import ai.shreds.shared.value_objects.SharedAddressValue;

/**
 * DTO representing a request to create a shipment.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SharedCreateShipmentRequestDTO implements Serializable {
    private static final long serialVersionUID = 1L;
    
    @NotBlank(message = "Order ID is required")
    private String orderId;
    
    @NotEmpty(message = "Items list cannot be empty")
    @Valid
    private List<SharedOrderItemDTO> items;
    
    @NotNull(message = "Shipping address is required")
    @Valid
    private SharedAddressValue shippingAddress;
    
    @Builder.Default
    private Boolean expedited = false;
    
    /**
     * Validates the shipment request data.
     * 
     * @throws IllegalArgumentException if validation fails
     */
    public void validate() {
        if (orderId == null || orderId.trim().isEmpty()) {
            throw new IllegalArgumentException("Order ID is required");
        }
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("Items list cannot be empty");
        }
        if (shippingAddress == null) {
            throw new IllegalArgumentException("Shipping address is required");
        }
        
        // Validate each item
        items.forEach(item -> {
            if (item == null) {
                throw new IllegalArgumentException("Order item cannot be null");
            }
        });
    }
    
    /**
     * Calculates the total number of items to be shipped.
     * 
     * @return total quantity of all items
     */
    public int getTotalItemCount() {
        return items != null ? items.stream().mapToInt(SharedOrderItemDTO::getQuantity).sum() : 0;
    }
    
    /**
     * Gets the number of unique products to be shipped.
     * 
     * @return count of unique products
     */
    public int getUniqueProductCount() {
        return items != null ? (int) items.stream().map(SharedOrderItemDTO::getProductId).distinct().count() : 0;
    }
    
    /**
     * Checks if expedited shipping is requested.
     * 
     * @return true if expedited shipping is requested
     */
    public boolean isExpedited() {
        return Boolean.TRUE.equals(expedited);
    }
    
    /**
     * Checks if shipment contains fragile items based on product IDs.
     * This is a basic implementation that can be enhanced with actual fragile item logic.
     * 
     * @return true if potentially fragile items are present
     */
    public boolean containsFragileItems() {
        if (items == null) {
            return false;
        }
        
        // Basic heuristic - items with "GLASS", "CERAMIC", "FRAGILE" in product ID
        return items.stream()
                .anyMatch(item -> {
                    String productId = item.getProductId();
                    return productId != null && (
                        productId.toUpperCase().contains("GLASS") ||
                        productId.toUpperCase().contains("CERAMIC") ||
                        productId.toUpperCase().contains("FRAGILE")
                    );
                });
    }
    
    /**
     * Factory method to create an expedited shipment request.
     * 
     * @param orderId the order ID
     * @param items the list of items
     * @param shippingAddress the shipping address
     * @return a new expedited SharedCreateShipmentRequestDTO
     */
    public static SharedCreateShipmentRequestDTO createExpedited(String orderId, List<SharedOrderItemDTO> items, SharedAddressValue shippingAddress) {
        return SharedCreateShipmentRequestDTO.builder()
                .orderId(orderId)
                .items(items)
                .shippingAddress(shippingAddress)
                .expedited(true)
                .build();
    }
    
    /**
     * Factory method to create a standard shipment request.
     * 
     * @param orderId the order ID
     * @param items the list of items
     * @param shippingAddress the shipping address
     * @return a new standard SharedCreateShipmentRequestDTO
     */
    public static SharedCreateShipmentRequestDTO createStandard(String orderId, List<SharedOrderItemDTO> items, SharedAddressValue shippingAddress) {
        return SharedCreateShipmentRequestDTO.builder()
                .orderId(orderId)
                .items(items)
                .shippingAddress(shippingAddress)
                .expedited(false)
                .build();
    }
}