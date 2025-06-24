package ai.shreds.domain.entities;

import ai.shreds.shared.enums.SharedOrderItemStatusEnum;
import ai.shreds.domain.exceptions.DomainValidationException;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "order_items")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DomainOrderItemEntity {

    @Id
    @Column(name = "order_item_id")
    private UUID orderItemId;

    @Column(name = "order_id", nullable = false)
    private UUID orderId;

    @Column(name = "product_id", nullable = false, length = 50)
    private String productId;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Column(name = "unit_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "total_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalPrice;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(name = "item_status", nullable = false, length = 20)
    private SharedOrderItemStatusEnum itemStatus;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (orderItemId == null) {
            orderItemId = UUID.randomUUID();
        }
        createdAt = LocalDateTime.now();
        if (itemStatus == null) {
            itemStatus = SharedOrderItemStatusEnum.PENDING;
        }
        if (currency == null) {
            currency = "USD";
        }
        calculateTotalPrice();
        validateBusinessRules();
    }

    @PreUpdate
    protected void onUpdate() {
        calculateTotalPrice();
        validateBusinessRules();
    }

    public void calculateTotalPrice() {
        if (unitPrice != null && quantity != null) {
            this.totalPrice = unitPrice.multiply(BigDecimal.valueOf(quantity));
        }
    }

    private void validateBusinessRules() {
        if (quantity == null || quantity <= 0) {
            throw new DomainValidationException("Quantity must be greater than zero", 
                List.of("quantity must be > 0"));
        }
        if (unitPrice == null || unitPrice.compareTo(BigDecimal.ZERO) < 0) {
            throw new DomainValidationException("Unit price cannot be negative", 
                List.of("unitPrice must be >= 0"));
        }
        if (productId == null || productId.trim().isEmpty()) {
            throw new DomainValidationException("Product ID is required", 
                List.of("productId cannot be null or empty"));
        }
        if (orderId == null) {
            throw new DomainValidationException("Order ID is required", 
                List.of("orderId cannot be null"));
        }
    }
}