package ai.shreds.domain.entities;

import ai.shreds.shared.value_objects.SharedMoneyValue;
import ai.shreds.shared.dtos.SharedReturnItemDTO;

import javax.persistence.*;
import java.util.Objects;

/**
 * Domain entity representing a return item within a return request.
 * Contains business logic for individual item returns.
 */
@Entity
@Table(name = "return_items")
public class DomainReturnItemEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "return_item_id")
    private Long id;
    
    @Column(name = "order_item_id", nullable = false, length = 50)
    private String orderItemId;
    
    @Column(name = "product_id", nullable = false, length = 50)
    private String productId;
    
    @Column(name = "quantity", nullable = false)
    private Integer quantity;
    
    @Column(name = "return_reason", nullable = false, length = 100)
    private String returnReason;
    
    @Column(name = "condition_desc", nullable = false, length = 50)
    private String condition;
    
    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "amount", column = @Column(name = "refund_amount")),
        @AttributeOverride(name = "currency", column = @Column(name = "refund_currency"))
    })
    private SharedMoneyValue refundAmount;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "return_id", nullable = false)
    private DomainReturnRequestEntity returnRequest;
    
    // Default constructor for JPA
    protected DomainReturnItemEntity() {}
    
    // Constructor for creating new return items
    public DomainReturnItemEntity(String orderItemId, String productId, Integer quantity, 
                                String returnReason, String condition) {
        this.orderItemId = Objects.requireNonNull(orderItemId, "Order item ID cannot be null");
        this.productId = Objects.requireNonNull(productId, "Product ID cannot be null");
        this.quantity = Objects.requireNonNull(quantity, "Quantity cannot be null");
        this.returnReason = Objects.requireNonNull(returnReason, "Return reason cannot be null");
        this.condition = Objects.requireNonNull(condition, "Condition cannot be null");
        
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
    }
    
    /**
     * Business logic to calculate refund amount based on item condition and reason.
     */
    public void calculateRefundAmount(SharedMoneyValue originalPrice, double restockingFeeRate) {
        if (originalPrice == null) {
            throw new IllegalArgumentException("Original price cannot be null");
        }
        
        double baseRefund = originalPrice.amount().doubleValue() * quantity;
        
        // Apply restocking fee based on condition and reason
        double restockingFee = 0.0;
        if (shouldApplyRestockingFee()) {
            restockingFee = baseRefund * restockingFeeRate;
        }
        
        double finalRefund = baseRefund - restockingFee;
        this.refundAmount = new SharedMoneyValue(
            java.math.BigDecimal.valueOf(Math.max(0, finalRefund)), 
            originalPrice.currency()
        );
    }
    
    /**
     * Determine if restocking fee should be applied based on condition and reason.
     */
    private boolean shouldApplyRestockingFee() {
        // No restocking fee for defective products or seller fault
        if ("DEFECTIVE_PRODUCT".equals(returnReason) || 
            "WRONG_ITEM".equals(returnReason) || 
            "DAMAGED_IN_SHIPPING".equals(returnReason) ||
            "NOT_AS_DESCRIBED".equals(returnReason)) {
            return false;
        }
        
        // Apply restocking fee for "CHANGED_MIND" if not in perfect condition
        if ("CHANGED_MIND".equals(returnReason)) {
            return !"PERFECT".equals(condition);
        }
        
        // Default behavior for other reasons
        return !"PERFECT".equals(condition);
    }
    
    /**
     * Validate the return item business rules.
     */
    public void validate() {
        if (quantity <= 0) {
            throw new IllegalStateException("Return quantity must be positive");
        }
        
        if (orderItemId == null || orderItemId.trim().isEmpty()) {
            throw new IllegalStateException("Order item ID cannot be null or empty");
        }
        
        if (productId == null || productId.trim().isEmpty()) {
            throw new IllegalStateException("Product ID cannot be null or empty");
        }
        
        if (returnReason == null || returnReason.trim().isEmpty()) {
            throw new IllegalStateException("Return reason cannot be null or empty");
        }
        
        if (condition == null || condition.trim().isEmpty()) {
            throw new IllegalStateException("Condition cannot be null or empty");
        }
    }
    
    /**
     * Convert to DTO for external communication.
     */
    public SharedReturnItemDTO toDTO() {
        SharedReturnItemDTO dto = new SharedReturnItemDTO();
        dto.setOrderItemId(this.orderItemId);
        dto.setProductId(this.productId);
        dto.setQuantity(this.quantity);
        dto.setReturnReason(this.returnReason);
        dto.setCondition(this.condition);
        dto.setRefundAmount(this.refundAmount);
        return dto;
    }
    
    /**
     * Create entity from DTO.
     */
    public static DomainReturnItemEntity fromDTO(SharedReturnItemDTO dto) {
        DomainReturnItemEntity entity = new DomainReturnItemEntity();
        entity.orderItemId = dto.getOrderItemId();
        entity.productId = dto.getProductId();
        entity.quantity = dto.getQuantity();
        entity.returnReason = dto.getReturnReason();
        entity.condition = dto.getCondition();
        entity.refundAmount = dto.getRefundAmount();
        return entity;
    }
    
    // Getters
    public Long getId() { return id; }
    public String getOrderItemId() { return orderItemId; }
    public String getProductId() { return productId; }
    public Integer getQuantity() { return quantity; }
    public String getReturnReason() { return returnReason; }
    public String getCondition() { return condition; }
    public SharedMoneyValue getRefundAmount() { return refundAmount; }
    public DomainReturnRequestEntity getReturnRequest() { return returnRequest; }
    
    // Setters for JPA and associations
    protected void setId(Long id) { this.id = id; }
    protected void setOrderItemId(String orderItemId) { this.orderItemId = orderItemId; }
    protected void setProductId(String productId) { this.productId = productId; }
    protected void setQuantity(Integer quantity) { this.quantity = quantity; }
    protected void setReturnReason(String returnReason) { this.returnReason = returnReason; }
    protected void setCondition(String condition) { this.condition = condition; }
    protected void setRefundAmount(SharedMoneyValue refundAmount) { this.refundAmount = refundAmount; }
    public void setReturnRequest(DomainReturnRequestEntity returnRequest) { this.returnRequest = returnRequest; }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DomainReturnItemEntity that = (DomainReturnItemEntity) o;
        return Objects.equals(orderItemId, that.orderItemId) && 
               Objects.equals(productId, that.productId);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(orderItemId, productId);
    }
    
    @Override
    public String toString() {
        return String.format("DomainReturnItemEntity{orderItemId='%s', productId='%s', quantity=%d}", 
                           orderItemId, productId, quantity);
    }
}