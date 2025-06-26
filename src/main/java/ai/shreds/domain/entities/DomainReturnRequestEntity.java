package ai.shreds.domain.entities;

import ai.shreds.shared.enums.SharedReturnReasonEnum;
import ai.shreds.shared.enums.SharedReturnStatusEnum;
import ai.shreds.shared.value_objects.SharedAddressValue;
import ai.shreds.shared.value_objects.SharedMoneyValue;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;

/**
 * Domain entity representing a return request.
 */
@Entity
@Table(name = "return_requests")
public class DomainReturnRequestEntity {

    @Id
    @Column(name = "return_id", nullable = false, length = 50)
    private String returnId;

    @Column(name = "order_id", nullable = false, length = 50)
    private String orderId;

    @Column(name = "customer_id", nullable = false, length = 50)
    private String customerId;

    @Column(name = "rma_number", nullable = false, unique = true, length = 50)
    private String rmaNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "reason", nullable = false)
    private SharedReturnReasonEnum reason;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private SharedReturnStatusEnum status;

    @OneToMany(mappedBy = "returnRequest", cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true)
    private List<DomainReturnItemEntity> returnItems = new ArrayList<>();

    @Column(name = "requested_at", nullable = false)
    private LocalDateTime requestedAt;

    @Column(name = "received_at")
    private LocalDateTime receivedAt;

    @Column(name = "refunded_at")
    private LocalDateTime refundedAt;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "value", column = @Column(name = "refund_amount")),
        @AttributeOverride(name = "currency", column = @Column(name = "refund_currency"))
    })
    private SharedMoneyValue refundAmount;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "street1", column = @Column(name = "return_street1")),
        @AttributeOverride(name = "street2", column = @Column(name = "return_street2")),
        @AttributeOverride(name = "city", column = @Column(name = "return_city")),
        @AttributeOverride(name = "state", column = @Column(name = "return_state")),
        @AttributeOverride(name = "postalCode", column = @Column(name = "return_postal_code")),
        @AttributeOverride(name = "country", column = @Column(name = "return_country"))
    })
    private SharedAddressValue returnAddress;

    @Column(name = "instructions", columnDefinition = "TEXT")
    private String instructions;

    @Version
    @Column(name = "version")
    private Integer version;

    // Default constructor for JPA
    protected DomainReturnRequestEntity() {}

    // Constructor
    public DomainReturnRequestEntity(String returnId, String orderId, String customerId, 
                                   String rmaNumber, SharedReturnReasonEnum reason,
                                   SharedReturnStatusEnum status, LocalDateTime requestedAt) {
        this.returnId = returnId;
        this.orderId = orderId;
        this.customerId = customerId;
        this.rmaNumber = rmaNumber;
        this.reason = reason;
        this.status = status;
        this.requestedAt = requestedAt;
    }

    // Getters
    public String getReturnId() {
        return returnId;
    }

    public String getOrderId() {
        return orderId;
    }

    public String getCustomerId() {
        return customerId;
    }

    public String getRmaNumber() {
        return rmaNumber;
    }

    public SharedReturnReasonEnum getReason() {
        return reason;
    }

    public SharedReturnStatusEnum getStatus() {
        return status;
    }

    public List<DomainReturnItemEntity> getReturnItems() {
        return returnItems;
    }

    public LocalDateTime getRequestedAt() {
        return requestedAt;
    }

    public LocalDateTime getReceivedAt() {
        return receivedAt;
    }

    public LocalDateTime getRefundedAt() {
        return refundedAt;
    }

    public SharedMoneyValue getRefundAmount() {
        return refundAmount;
    }

    public SharedAddressValue getReturnAddress() {
        return returnAddress;
    }

    public String getInstructions() {
        return instructions;
    }

    public Integer getVersion() {
        return version;
    }

    // Setters
    public void setStatus(SharedReturnStatusEnum status) {
        this.status = status;
    }

    public void setReceivedAt(LocalDateTime receivedAt) {
        this.receivedAt = receivedAt;
    }

    public void setRefundedAt(LocalDateTime refundedAt) {
        this.refundedAt = refundedAt;
    }

    public void setRefundAmount(SharedMoneyValue refundAmount) {
        this.refundAmount = refundAmount;
    }

    public void setReturnAddress(SharedAddressValue returnAddress) {
        this.returnAddress = returnAddress;
    }

    public void setInstructions(String instructions) {
        this.instructions = instructions;
    }

    public void addReturnItem(DomainReturnItemEntity item) {
        returnItems.add(item);
        item.setReturnRequest(this);
    }

    public void removeReturnItem(DomainReturnItemEntity item) {
        returnItems.remove(item);
        item.setReturnRequest(null);
    }
}