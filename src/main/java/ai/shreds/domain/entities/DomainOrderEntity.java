package ai.shreds.domain.entities;

import ai.shreds.shared.enums.SharedOrderStatusEnum;
import ai.shreds.domain.value_objects.DomainOrderAggregate;
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
@Table(name = "orders")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DomainOrderEntity {

    @Id
    @Column(name = "order_id")
    private UUID orderId;

    @Column(name = "order_number", unique = true, nullable = false, length = 30)
    private String orderNumber;

    @Column(name = "cart_id", unique = true, nullable = false, length = 50)
    private String cartId;

    @Column(name = "customer_id", nullable = false, length = 50)
    private String customerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "order_status", nullable = false, length = 20)
    private SharedOrderStatusEnum orderStatus;

    @Column(name = "order_date", nullable = false)
    private LocalDateTime orderDate;

    @Column(name = "subtotal_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotalAmount;

    @Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Column(name = "billing_address_id", nullable = false)
    private UUID billingAddressId;

    @Column(name = "shipping_address_id", nullable = false)
    private UUID shippingAddressId;

    @Version
    @Column(name = "version")
    private Integer version;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Removed OneToMany relationship that was causing persistence conflicts
    // Order items are managed separately by the repository

    @PrePersist
    protected void onCreate() {
        if (orderId == null) {
            orderId = UUID.randomUUID();
        }
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (orderStatus == null) {
            orderStatus = SharedOrderStatusEnum.PENDING;
        }
        if (currency == null) {
            currency = "USD";
        }
        applyBusinessRules();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
        applyBusinessRules();
    }

    public void applyBusinessRules() {
        if (subtotalAmount != null && subtotalAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new DomainValidationException("Subtotal amount cannot be negative", 
                List.of("subtotalAmount must be >= 0"));
        }
        if (totalAmount != null && totalAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new DomainValidationException("Total amount cannot be negative", 
                List.of("totalAmount must be >= 0"));
        }
        if (customerId == null || customerId.trim().isEmpty()) {
            throw new DomainValidationException("Customer ID is required", 
                List.of("customerId cannot be null or empty"));
        }
        if (cartId == null || cartId.trim().isEmpty()) {
            throw new DomainValidationException("Cart ID is required", 
                List.of("cartId cannot be null or empty"));
        }
    }

    public DomainOrderAggregate toAggregate(List<DomainOrderItemEntity> orderItems,
                                          DomainPaymentDetailsEntity paymentDetails,
                                          DomainShippingDetailsEntity shippingDetails,
                                          DomainAddressEntity billingAddress,
                                          DomainAddressEntity shippingAddress) {
        return DomainOrderAggregate.builder()
            .order(this)
            .orderItems(orderItems)
            .paymentDetails(paymentDetails)
            .shippingDetails(shippingDetails)
            .billingAddress(billingAddress)
            .shippingAddress(shippingAddress)
            .build();
    }
}