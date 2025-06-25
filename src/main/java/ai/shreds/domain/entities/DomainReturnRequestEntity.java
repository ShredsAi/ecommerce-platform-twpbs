package ai.shreds.domain.entities;

import ai.shreds.shared.enums.SharedReturnReasonEnum;
import ai.shreds.shared.enums.SharedReturnStatusEnum;
import ai.shreds.shared.value_objects.SharedMoneyValue;
import ai.shreds.shared.value_objects.SharedAddressValue;
import ai.shreds.shared.dtos.SharedReturnRequestDTO;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.math.BigDecimal;

/**
 * Domain entity representing a return request aggregate.
 * Contains business logic for return lifecycle management and contains return items.
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
    private List<DomainReturnItemEntity> items;
    
    @Column(name = "requested_at", nullable = false)
    private LocalDateTime requestedAt;
    
    @Column(name = "received_at")
    private LocalDateTime receivedAt;
    
    @Column(name = "refunded_at")
    private LocalDateTime refundedAt;
    
    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "amount", column = @Column(name = "refund_amount")),
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
    private Long version;
    
    // Default constructor for JPA
    protected DomainReturnRequestEntity() {
        this.items = new ArrayList<>();
    }
    
    // Constructor for creating new return requests
    public DomainReturnRequestEntity(String returnId, String orderId, String customerId, 
                                   String rmaNumber, SharedReturnReasonEnum reason, 
                                   SharedAddressValue returnAddress) {
        this.returnId = Objects.requireNonNull(returnId, "Return ID cannot be null");
        this.orderId = Objects.requireNonNull(orderId, "Order ID cannot be null");
        this.customerId = Objects.requireNonNull(customerId, "Customer ID cannot be null");
        this.rmaNumber = Objects.requireNonNull(rmaNumber, "RMA number cannot be null");
        this.reason = Objects.requireNonNull(reason, "Reason cannot be null");
        this.returnAddress = Objects.requireNonNull(returnAddress, "Return address cannot be null");
        this.status = SharedReturnStatusEnum.REQUESTED;
        this.requestedAt = LocalDateTime.now();
        this.items = new ArrayList<>();
        this.version = 0L;
        this.instructions = "Please package items securely and include RMA number: " + rmaNumber;
    }
    
    /**
     * Business logic to add an item to the return request.
     */
    public void addItem(DomainReturnItemEntity item) {
        Objects.requireNonNull(item, "Return item cannot be null");
        
        // Check if item already exists
        boolean itemExists = this.items.stream()
            .anyMatch(existingItem -> 
                existingItem.getOrderItemId().equals(item.getOrderItemId()) && 
                existingItem.getProductId().equals(item.getProductId())
            );
        
        if (itemExists) {
            throw new IllegalArgumentException(
                String.format("Item with orderItemId %s and productId %s already exists in return request", 
                            item.getOrderItemId(), item.getProductId())
            );
        }
        
        item.validate();
        item.setReturnRequest(this);
        this.items.add(item);
        
        // Recalculate total refund amount
        calculateRefundAmount();
    }
    
    /**
     * Business logic to update return status with proper state transitions.
     */
    public void updateStatus(SharedReturnStatusEnum newStatus) {
        Objects.requireNonNull(newStatus, "New status cannot be null");
        
        if (!isValidStatusTransition(this.status, newStatus)) {
            throw new IllegalStateException(
                String.format("Invalid status transition from %s to %s", this.status, newStatus)
            );
        }
        
        SharedReturnStatusEnum oldStatus = this.status;
        this.status = newStatus;
        
        // Set timestamps based on status
        switch (newStatus) {
            case RECEIVED:
                this.receivedAt = LocalDateTime.now();
                break;
            case REFUNDED:
                this.refundedAt = LocalDateTime.now();
                break;
            case CLOSED:
                if (this.refundedAt == null) {
                    this.refundedAt = LocalDateTime.now();
                }
                break;
        }
    }
    
    /**
     * Validate status transitions according to business rules.
     */
    private boolean isValidStatusTransition(SharedReturnStatusEnum currentStatus, SharedReturnStatusEnum newStatus) {
        if (currentStatus == newStatus) {
            return true; // Allow idempotent operations
        }
        
        switch (currentStatus) {
            case REQUESTED:
                return newStatus == SharedReturnStatusEnum.APPROVED || 
                       newStatus == SharedReturnStatusEnum.REJECTED;
            case APPROVED:
                return newStatus == SharedReturnStatusEnum.IN_TRANIST || 
                       newStatus == SharedReturnStatusEnum.REJECTED;
            case IN_TRANIST:
                return newStatus == SharedReturnStatusEnum.RECEIVED;
            case RECEIVED:
                return newStatus == SharedReturnStatusEnum.PROCESSING;
            case PROCESSING:
                return newStatus == SharedReturnStatusEnum.REFUNDED || 
                       newStatus == SharedReturnStatusEnum.REJECTED;
            case REFUNDED:
                return newStatus == SharedReturnStatusEnum.CLOSED;
            case REJECTED:
            case CLOSED:
                return false; // Final states
            default:
                return false;
        }
    }
    
    /**
     * Business logic to calculate total refund amount from all items.
     */
    public SharedMoneyValue calculateRefundAmount() {
        if (items.isEmpty()) {
            this.refundAmount = new SharedMoneyValue(BigDecimal.ZERO, "USD");
            return this.refundAmount;
        }
        
        BigDecimal totalAmount = BigDecimal.ZERO;
        String currency = null;
        
        for (DomainReturnItemEntity item : items) {
            if (item.getRefundAmount() != null) {
                if (currency == null) {
                    currency = item.getRefundAmount().currency();
                } else if (!currency.equals(item.getRefundAmount().currency())) {
                    throw new IllegalStateException("All return items must have the same currency");
                }
                totalAmount = totalAmount.add(item.getRefundAmount().amount());
            }
        }
        
        if (currency == null) {
            currency = "USD"; // Default currency
        }
        
        this.refundAmount = new SharedMoneyValue(totalAmount, currency);
        return this.refundAmount;
    }
    
    /**
     * Check if return can be processed further.
     */
    public boolean canBeProcessed() {
        return this.status == SharedReturnStatusEnum.REQUESTED || 
               this.status == SharedReturnStatusEnum.APPROVED || 
               this.status == SharedReturnStatusEnum.IN_TRANIST || 
               this.status == SharedReturnStatusEnum.RECEIVED || 
               this.status == SharedReturnStatusEnum.PROCESSING;
    }
    
    /**
     * Check if this return is in a final state.
     */
    public boolean isFinalState() {
        return this.status == SharedReturnStatusEnum.REFUNDED || 
               this.status == SharedReturnStatusEnum.REJECTED || 
               this.status == SharedReturnStatusEnum.CLOSED;
    }
    
    /**
     * Check if return is within the allowed return period.
     */
    public boolean isWithinReturnPeriod(int standardReturnDays, boolean isHolidayExtension, int holidayExtensionDays) {
        if (requestedAt == null) {
            return false;
        }
        
        int effectiveDays = standardReturnDays;
        if (isHolidayExtension) {
            effectiveDays += holidayExtensionDays;
        }
        
        LocalDateTime deadline = requestedAt.plusDays(effectiveDays);
        return LocalDateTime.now().isBefore(deadline);
    }
    
    /**
     * Generate return instructions based on return reason and items.
     */
    public void generateInstructions() {
        StringBuilder instructionsBuilder = new StringBuilder();
        instructionsBuilder.append("Return Instructions for RMA: ").append(rmaNumber).append("\n\n");
        
        instructionsBuilder.append("Please follow these steps:\n");
        instructionsBuilder.append("1. Package all items securely in original packaging if available\n");
        instructionsBuilder.append("2. Include this RMA number (" + rmaNumber + ") inside the package\n");
        instructionsBuilder.append("3. Print and attach the return shipping label\n");
        instructionsBuilder.append("4. Drop off at authorized shipping location\n\n");
        
        if (reason == SharedReturnReasonEnum.DEFECTIVE_PRODUCT) {
            instructionsBuilder.append("IMPORTANT: As this is a defective product return, please include a description of the defect.\n");
        }
        
        instructionsBuilder.append("Return Address:\n");
        instructionsBuilder.append(returnAddress.toString());
        
        this.instructions = instructionsBuilder.toString();
    }
    
    /**
     * Convert to DTO for external communication.
     */
    public SharedReturnRequestDTO toDTO() {
        SharedReturnRequestDTO dto = new SharedReturnRequestDTO();
        dto.setReturnId(this.returnId);
        dto.setOrderId(this.orderId);
        dto.setCustomerId(this.customerId);
        dto.setRmaNumber(this.rmaNumber);
        dto.setReason(this.reason);
        dto.setStatus(this.status);
        dto.setItems(this.items.stream().map(DomainReturnItemEntity::toDTO).toList());
        dto.setRequestedAt(this.requestedAt);
        dto.setReceivedAt(this.receivedAt);
        dto.setRefundedAt(this.refundedAt);
        dto.setRefundAmount(this.refundAmount);
        dto.setReturnAddress(this.returnAddress);
        dto.setInstructions(this.instructions);
        return dto;
    }
    
    /**
     * Create entity from DTO.
     */
    public static DomainReturnRequestEntity fromDTO(SharedReturnRequestDTO dto) {
        DomainReturnRequestEntity entity = new DomainReturnRequestEntity();
        entity.returnId = dto.getReturnId();
        entity.orderId = dto.getOrderId();
        entity.customerId = dto.getCustomerId();
        entity.rmaNumber = dto.getRmaNumber();
        entity.reason = dto.getReason();
        entity.status = dto.getStatus();
        entity.requestedAt = dto.getRequestedAt();
        entity.receivedAt = dto.getReceivedAt();
        entity.refundedAt = dto.getRefundedAt();
        entity.refundAmount = dto.getRefundAmount();
        entity.returnAddress = dto.getReturnAddress();
        entity.instructions = dto.getInstructions();
        
        if (dto.getItems() != null) {
            entity.items = dto.getItems().stream()
                .map(DomainReturnItemEntity::fromDTO)
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
        }
        
        return entity;
    }
    
    // Getters
    public String getReturnId() { return returnId; }
    public String getOrderId() { return orderId; }
    public String getCustomerId() { return customerId; }
    public String getRmaNumber() { return rmaNumber; }
    public SharedReturnReasonEnum getReason() { return reason; }
    public SharedReturnStatusEnum getStatus() { return status; }
    public List<DomainReturnItemEntity> getItems() { return new ArrayList<>(items); }
    public LocalDateTime getRequestedAt() { return requestedAt; }
    public LocalDateTime getReceivedAt() { return receivedAt; }
    public LocalDateTime getRefundedAt() { return refundedAt; }
    public SharedMoneyValue getRefundAmount() { return refundAmount; }
    public SharedAddressValue getReturnAddress() { return returnAddress; }
    public String getInstructions() { return instructions; }
    public Long getVersion() { return version; }
    
    // Setters for JPA
    protected void setReturnId(String returnId) { this.returnId = returnId; }
    protected void setOrderId(String orderId) { this.orderId = orderId; }
    protected void setCustomerId(String customerId) { this.customerId = customerId; }
    protected void setRmaNumber(String rmaNumber) { this.rmaNumber = rmaNumber; }
    protected void setReason(SharedReturnReasonEnum reason) { this.reason = reason; }
    protected void setStatus(SharedReturnStatusEnum status) { this.status = status; }
    protected void setItems(List<DomainReturnItemEntity> items) { this.items = items; }
    protected void setRequestedAt(LocalDateTime requestedAt) { this.requestedAt = requestedAt; }
    protected void setReceivedAt(LocalDateTime receivedAt) { this.receivedAt = receivedAt; }
    protected void setRefundedAt(LocalDateTime refundedAt) { this.refundedAt = refundedAt; }
    protected void setRefundAmount(SharedMoneyValue refundAmount) { this.refundAmount = refundAmount; }
    protected void setReturnAddress(SharedAddressValue returnAddress) { this.returnAddress = returnAddress; }
    protected void setInstructions(String instructions) { this.instructions = instructions; }
    protected void setVersion(Long version) { this.version = version; }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DomainReturnRequestEntity that = (DomainReturnRequestEntity) o;
        return Objects.equals(returnId, that.returnId);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(returnId);
    }
    
    @Override
    public String toString() {
        return String.format("DomainReturnRequestEntity{returnId='%s', orderId='%s', rmaNumber='%s', status=%s}", 
                           returnId, orderId, rmaNumber, status);
    }
}