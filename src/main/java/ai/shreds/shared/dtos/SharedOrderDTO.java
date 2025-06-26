package ai.shreds.shared.dtos;

import java.util.UUID;
import java.util.List;
import java.io.Serializable;
import java.math.BigDecimal;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotEmpty;

import ai.shreds.shared.value_objects.SharedMoneyValue;
import ai.shreds.shared.enums.SharedOrderStatusEnum;

/**
 * DTO representing an order with summary information shared across services.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SharedOrderDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    @NotNull(message = "Order ID is required")
    private UUID orderId;
    
    @NotBlank(message = "Customer ID is required")
    private String customerId;
    
    @NotBlank(message = "Order number is required")
    private String orderNumber;
    
    @NotBlank(message = "Order status is required")
    private String orderStatus;
    
    @NotNull(message = "Total amount is required")
    @Valid
    private SharedMoneyValue totalAmount;
    
    @NotEmpty(message = "Order items cannot be empty")
    @Valid
    private List<SharedOrderItemDTO> items;
    
    /**
     * Validates the order data.
     *
     * @throws IllegalArgumentException if validation fails
     */
    public void validate() {
        if (orderId == null) {
            throw new IllegalArgumentException("Order ID is required");
        }
        if (customerId == null || customerId.trim().isEmpty()) {
            throw new IllegalArgumentException("Customer ID is required");
        }
        if (orderNumber == null || orderNumber.trim().isEmpty()) {
            throw new IllegalArgumentException("Order number is required");
        }
        if (orderStatus == null || orderStatus.trim().isEmpty()) {
            throw new IllegalArgumentException("Order status is required");
        }
        if (totalAmount == null) {
            throw new IllegalArgumentException("Total amount is required");
        }
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("Order items cannot be empty");
        }
        items.forEach(item -> {
            if (item == null) {
                throw new IllegalArgumentException("Order item cannot be null");
            }
        });
    }
    
    public int getTotalItemCount() {
        return items != null ? items.stream().mapToInt(SharedOrderItemDTO::getQuantity).sum() : 0;
    }
    
    public int getUniqueProductCount() {
        return items != null ? (int) items.stream().map(SharedOrderItemDTO::getProductId).distinct().count() : 0;
    }
    
    public boolean isCompleted() {
        return "COMPLETED".equalsIgnoreCase(orderStatus) ||
               "DELIVERED".equalsIgnoreCase(orderStatus);
    }
    
    public boolean isCancelled() {
        return "CANCELLED".equalsIgnoreCase(orderStatus);
    }
    
    public boolean isInProgress() {
        return !isCompleted() && !isCancelled();
    }
    
    public boolean verifyTotals() {
        if (items == null || totalAmount == null) {
            return false;
        }
        BigDecimal calculatedTotal = items.stream()
            .filter(item -> item.getTotalPrice() != null)
            .map(item -> item.getTotalPrice().getValue())
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        return totalAmount.getValue().compareTo(calculatedTotal) == 0;
    }
}
