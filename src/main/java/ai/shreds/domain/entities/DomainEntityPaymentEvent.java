package ai.shreds.domain.entities;

import ai.shreds.shared.enums.SharedEnumPaymentEventType;
import ai.shreds.shared.value_objects.SharedValueMoney;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Domain event entity representing a payment event to publish and audit.
 */
@Entity
@Table(name = "payment_events")
public class DomainEntityPaymentEvent {
    
    @Id
    @Column(name = "id")
    private UUID id;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false)
    private SharedEnumPaymentEventType eventType;
    
    @Column(name = "payment_id", nullable = false)
    private UUID paymentId;
    
    @Column(name = "payment_intent_id")
    private String paymentIntentId;
    
    @Column(name = "customer_id")
    private UUID customerId;
    
    @Column(name = "order_id")
    private UUID orderId;
    
    @Column(name = "amount_value", precision = 19, scale = 2)
    private BigDecimal amountValue;
    
    @Column(name = "amount_currency")
    private String amountCurrency;
    
    @Column(name = "event_data", columnDefinition = "TEXT")
    private String eventData;
    
    @Column(name = "correlation_id")
    private String correlationId;
    
    @Column(name = "published_at")
    private LocalDateTime publishedAt;
    
    @Column(name = "webhook_id")
    private UUID webhookId;

    // Default constructor for JPA
    protected DomainEntityPaymentEvent() {}

    public DomainEntityPaymentEvent(
            UUID id,
            SharedEnumPaymentEventType eventType,
            UUID paymentId,
            String paymentIntentId,
            UUID customerId,
            UUID orderId,
            BigDecimal amountValue,
            String amountCurrency,
            String eventData,
            String correlationId,
            LocalDateTime publishedAt,
            UUID webhookId) {
        this.id = id;
        this.eventType = eventType;
        this.paymentId = paymentId;
        this.paymentIntentId = paymentIntentId;
        this.customerId = customerId;
        this.orderId = orderId;
        this.amountValue = amountValue;
        this.amountCurrency = amountCurrency;
        this.eventData = eventData;
        this.correlationId = correlationId;
        this.publishedAt = publishedAt;
        this.webhookId = webhookId;
    }

    public UUID getId() {
        return id;
    }

    public SharedEnumPaymentEventType getEventType() {
        return eventType;
    }

    public UUID getPaymentId() {
        return paymentId;
    }

    public String getPaymentIntentId() {
        return paymentIntentId;
    }

    public UUID getCustomerId() {
        return customerId;
    }

    public UUID getOrderId() {
        return orderId;
    }

    public BigDecimal getAmountValue() {
        return amountValue;
    }

    public String getAmountCurrency() {
        return amountCurrency;
    }

    public String getEventData() {
        return eventData;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public LocalDateTime getPublishedAt() {
        return publishedAt;
    }

    public UUID getWebhookId() {
        return webhookId;
    }

    /**
     * Returns a value object representing the monetary amount.
     */
    public SharedValueMoney getAmount() {
        return new SharedValueMoney(amountValue, amountCurrency);
    }
}