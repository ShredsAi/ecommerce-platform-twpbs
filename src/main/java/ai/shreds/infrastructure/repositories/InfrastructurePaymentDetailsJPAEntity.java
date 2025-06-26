package ai.shreds.infrastructure.repositories;

import lombok.*;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "payment_details")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InfrastructurePaymentDetailsJPAEntity {

    @Id
    @Column(name = "order_id", nullable = false, updatable = false)
    private UUID orderId;

    @Column(name = "payment_status", nullable = false)
    private String paymentStatus;

    @Column(name = "transaction_id", nullable = false)
    private String transactionId;

    @Column(name = "authorization_code", nullable = false)
    private String authorizationCode;

    @Column(name = "amount", nullable = false)
    private BigDecimal amount;

    @Column(name = "currency", nullable = false)
    private String currency;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
