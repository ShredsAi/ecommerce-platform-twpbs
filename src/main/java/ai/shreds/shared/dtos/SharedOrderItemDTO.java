package ai.shreds.shared.dtos;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import ai.shreds.shared.value_objects.SharedMoneyValue;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * DTO representing an order item within an order shared across services.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SharedOrderItemDTO implements Serializable {
    private static final long serialVersionUID = 1L;
    
    @NotBlank(message = "Order item ID is required")
    private String orderItemId;
    
    @NotBlank(message = "Product ID is required")
    private String productId;
    
    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be at least 1")
    private Integer quantity;
    
    @NotNull(message = "Unit price is required")
    @Valid
    private SharedMoneyValue unitPrice;
    
    @Valid
    private SharedMoneyValue totalPrice;
    
    /**
     * Calculates the total price based on quantity and unit price.
     * If total price is already set, it is verified against the calculation.
     * 
     * @return SharedMoneyValue representing the total price
     */
    public SharedMoneyValue calculateTotalPrice() {
        if (unitPrice == null || quantity == null) {
            throw new IllegalStateException("Unit price and quantity must be set to calculate total price");
        }
        
        SharedMoneyValue calculated = unitPrice.multiply(BigDecimal.valueOf(quantity));
        
        if (totalPrice == null) {
            totalPrice = calculated;
        } else if (!totalPrice.getValue().equals(calculated.getValue())) {
            throw new IllegalStateException("Provided total price does not match calculated value");
        }
        
        return totalPrice;
    }
    
    /**
     * Validates the order item data.
     * 
     * @throws IllegalArgumentException if validation fails
     */
    public void validate() {
        if (orderItemId == null || orderItemId.trim().isEmpty()) {
            throw new IllegalArgumentException("Order item ID is required");
        }
        if (productId == null || productId.trim().isEmpty()) {
            throw new IllegalArgumentException("Product ID is required");
        }
        if (quantity == null || quantity < 1) {
            throw new IllegalArgumentException("Quantity must be at least 1");
        }
        if (unitPrice == null) {
            throw new IllegalArgumentException("Unit price is required");
        }
        if (totalPrice == null) {
            calculateTotalPrice(); // Set total price if not provided
        }
    }
    
    /**
     * Checks if this order item is for the specified product.
     * 
     * @param prodId the product ID to check
     * @return true if this item's product ID matches the specified one
     */
    public boolean isForProduct(String prodId) {
        return productId != null && productId.equals(prodId);
    }
}
