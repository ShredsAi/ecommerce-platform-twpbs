package ai.shreds.shared.dtos;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotEmpty;
import java.util.UUID;
import java.util.List;
import ai.shreds.shared.value_objects.SharedMoneyValue;
import ai.shreds.shared.value_objects.SharedAddressValue;
import ai.shreds.shared.dtos.SharedOrderItemDTO;
import ai.shreds.application.dtos.ApplicationOrderCreatedDTO;

/**
 * DTO representing an order created event shared across services.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SharedOrderCreatedEventDTO {

    @NotNull(message = "Order ID is required")
    private UUID orderId;
    
    @NotBlank(message = "Status is required")
    private String status;
    
    @NotNull(message = "Total amount is required")
    @Valid
    private SharedMoneyValue totalAmount;
    
    @NotBlank(message = "Customer ID is required")
    private String customerId;
    
    @NotEmpty(message = "Order items cannot be empty")
    @Valid
    private List<SharedOrderItemDTO> items;
    
    @NotNull(message = "Billing address is required")
    @Valid
    private SharedAddressValue billingAddress;
    
    @NotNull(message = "Shipping address is required")
    @Valid
    private SharedAddressValue shippingAddress;

    /**
     * Converts this shared DTO to application layer DTO.
     * 
     * @return ApplicationOrderCreatedDTO with the same data
     */
    public ApplicationOrderCreatedDTO toApplicationDTO() {
        return ApplicationOrderCreatedDTO.fromSharedDTO(this);
    }

    /**
     * Validates the order created event data.
     * 
     * @throws IllegalArgumentException if validation fails
     */
    public void validate() {
        if (orderId == null) {
            throw new IllegalArgumentException("Order ID is required");
        }
        if (status == null || status.trim().isEmpty()) {
            throw new IllegalArgumentException("Status is required");
        }
        if (totalAmount == null) {
            throw new IllegalArgumentException("Total amount is required");
        }
        if (customerId == null || customerId.trim().isEmpty()) {
            throw new IllegalArgumentException("Customer ID is required");
        }
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("Order items cannot be empty");
        }
        if (billingAddress == null) {
            throw new IllegalArgumentException("Billing address is required");
        }
        if (shippingAddress == null) {
            throw new IllegalArgumentException("Shipping address is required");
        }
    }
    
    /**
     * Calculates the total number of items in the order.
     * 
     * @return total quantity of all items
     */
    public int getTotalItemCount() {
        return items != null ? items.stream().mapToInt(SharedOrderItemDTO::getQuantity).sum() : 0;
    }
    
    /**
     * Checks if the order has the same billing and shipping address.
     * 
     * @return true if addresses are the same
     */
    public boolean hasSameAddresses() {
        return billingAddress != null && billingAddress.equals(shippingAddress);
    }
    
    /**
     * Checks if the order status is pending.
     * 
     * @return true if status is PENDING
     */
    public boolean isPending() {
        return "PENDING".equalsIgnoreCase(status);
    }
}