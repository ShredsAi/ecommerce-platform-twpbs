package ai.shreds.infrastructure.repositories;

import lombok.*;
import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "shipping_details")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InfrastructureShippingDetailsJPAEntity {

    @Id
    @Column(name = "order_id", nullable = false, updatable = false)
    private UUID orderId;

    @Column(name = "shipping_status", nullable = false)
    private String shippingStatus;

    @Column(name = "tracking_number")
    private String trackingNumber;

    @Column(name = "carrier")
    private String carrier;

    @Column(name = "estimated_delivery_date")
    private LocalDate estimatedDeliveryDate;

    @Column(name = "actual_delivery_date")
    private LocalDate actualDeliveryDate;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
