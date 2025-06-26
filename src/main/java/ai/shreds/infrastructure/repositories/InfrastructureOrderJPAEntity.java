package ai.shreds.infrastructure.repositories;

import lombok.*;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "orders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InfrastructureOrderJPAEntity {

    @Id
    @Column(name = "order_id", nullable = false, updatable = false)
    private UUID orderId;

    @Column(name = "customer_id", nullable = false)
    private String customerId;

    @Column(name = "order_number", nullable = false)
    private String orderNumber;

    @Column(name = "order_status", nullable = false)
    private String orderStatus;

    @Column(name = "total_amount", nullable = false)
    private BigDecimal totalAmount;

    @Column(name = "currency", nullable = false)
    private String currency;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private Integer version;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<InfrastructureOrderItemJPAEntity> orderItems;

    @Column(name = "billing_street", nullable = false)
    private String billingStreet;

    @Column(name = "billing_city", nullable = false)
    private String billingCity;

    @Column(name = "billing_postal_code", nullable = false)
    private String billingPostalCode;

    @Column(name = "billing_country", nullable = false)
    private String billingCountry;

    @Column(name = "shipping_street", nullable = false)
    private String shippingStreet;

    @Column(name = "shipping_city", nullable = false)
    private String shippingCity;

    @Column(name = "shipping_postal_code", nullable = false)
    private String shippingPostalCode;

    @Column(name = "shipping_country", nullable = false)
    private String shippingCountry;
}