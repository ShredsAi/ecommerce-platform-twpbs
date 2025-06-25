package ai.shreds.domain.entities;

import ai.shreds.shared.enums.SharedShippingStatusEnum;
import ai.shreds.domain.exceptions.DomainValidationException;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "shipping_details")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DomainShippingDetailsEntity {

    @Id
    @Column(name = "shipping_id")
    private UUID shippingId;

    @Column(name = "order_id", unique = true, nullable = false)
    private UUID orderId;

    @Enumerated(EnumType.STRING)
    @Column(name = "shipping_status", nullable = false, length = 20)
    private SharedShippingStatusEnum shippingStatus;

    @Column(name = "tracking_number", length = 100)
    private String trackingNumber;

    @Column(name = "carrier", length = 50)
    private String carrier;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (shippingId == null) {
            shippingId = UUID.randomUUID();
        }
        createdAt = LocalDateTime.now();
        if (shippingStatus == null) {
            shippingStatus = SharedShippingStatusEnum.PENDING;
        }
        validateBusinessRules();
    }

    @PreUpdate
    protected void onUpdate() {
        validateBusinessRules();
    }

    private void validateBusinessRules() {
        if (orderId == null) {
            throw new DomainValidationException("Order ID is required", 
                List.of("orderId cannot be null"));
        }
        if (shippingStatus == SharedShippingStatusEnum.SHIPPED && 
            (trackingNumber == null || trackingNumber.trim().isEmpty())) {
            throw new DomainValidationException("Tracking number is required when status is SHIPPED", 
                List.of("trackingNumber cannot be null or empty when status is SHIPPED"));
        }
    }
}