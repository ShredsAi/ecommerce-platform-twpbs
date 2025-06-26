package ai.shreds.domain.entities;

import ai.shreds.domain.value_objects.DomainProductIdValue;
import ai.shreds.domain.value_objects.DomainQuantityValue;
import ai.shreds.shared.value_objects.SharedMoneyValue;
import ai.shreds.shared.enums.SharedOrderItemStatusEnum;
import ai.shreds.shared.dtos.SharedOrderItemDTO;
import java.math.BigDecimal;
import java.util.UUID;

public class DomainOrderItemEntity {
    private final String orderItemId;
    private final DomainProductIdValue productId;
    private final DomainQuantityValue quantity;
    private SharedMoneyValue unitPrice;
    private SharedMoneyValue totalPrice;
    private SharedOrderItemStatusEnum itemStatus;

    public DomainOrderItemEntity(String orderItemId,
                                 DomainProductIdValue productId,
                                 DomainQuantityValue quantity,
                                 SharedMoneyValue unitPrice,
                                 SharedMoneyValue totalPrice,
                                 SharedOrderItemStatusEnum itemStatus) {
        if (orderItemId == null || orderItemId.isBlank()) {
            throw new IllegalArgumentException("orderItemId cannot be null or blank");
        }
        this.orderItemId = orderItemId;
        this.productId = productId;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.totalPrice = totalPrice;
        this.itemStatus = itemStatus;
    }

    public String getOrderItemId() {
        return orderItemId;
    }

    public DomainProductIdValue getProductId() {
        return productId;
    }

    public DomainQuantityValue getQuantity() {
        return quantity;
    }

    public SharedMoneyValue getUnitPrice() {
        return unitPrice;
    }

    public SharedMoneyValue getTotalPrice() {
        return totalPrice;
    }

    public SharedOrderItemStatusEnum getItemStatus() {
        return itemStatus;
    }

    /**
     * Recalculates totalPrice = unitPrice * quantity.
     */
    public void calculateTotalPrice() {
        BigDecimal value = unitPrice.getValue()
            .multiply(BigDecimal.valueOf(quantity.getValue()));
        this.totalPrice = new SharedMoneyValue(value, unitPrice.getCurrency());
    }

    /**
     * Convert this domain entity to shared DTO.
     */
    public SharedOrderItemDTO toSharedDTO() {
        SharedOrderItemDTO dto = new SharedOrderItemDTO();
        dto.setOrderItemId(this.orderItemId);
        dto.setProductId(this.productId.getValue());
        dto.setQuantity(this.quantity.getValue());
        dto.setUnitPrice(this.unitPrice);
        dto.setTotalPrice(this.totalPrice);
        return dto;
    }

    /**
     * Create a domain entity from shared DTO.
     */
    public static DomainOrderItemEntity fromSharedDTO(SharedOrderItemDTO dto) {
        DomainProductIdValue pid = new DomainProductIdValue(dto.getProductId());
        DomainQuantityValue qty = new DomainQuantityValue(dto.getQuantity());
        SharedMoneyValue unit = dto.getUnitPrice();
        SharedMoneyValue total = dto.getTotalPrice();
        return new DomainOrderItemEntity(
            dto.getOrderItemId(),
            pid,
            qty,
            unit,
            total,
            SharedOrderItemStatusEnum.PENDING
        );
    }
}