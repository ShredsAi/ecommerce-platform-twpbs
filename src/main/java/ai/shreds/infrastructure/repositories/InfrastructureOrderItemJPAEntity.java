package ai.shreds.infrastructure.repositories;

import lombok.*;
import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "order_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InfrastructureOrderItemJPAEntity {

    @Id
    @Column(name = "order_item_id", nullable = false, updatable = false)
    private String orderItemId;

    @Column(name = "product_id", nullable = false)
    private String productId;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Column(name = "unit_price", nullable = false)
    private BigDecimal unitPrice;

    @Column(name = "total_price", nullable = false)
    private BigDecimal totalPrice;

    @Column(name = "item_status", nullable = false)
    private String itemStatus;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private InfrastructureOrderJPAEntity order;
}