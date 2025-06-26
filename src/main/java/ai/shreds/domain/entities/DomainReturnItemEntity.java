package ai.shreds.domain.entities;

import ai.shreds.shared.value_objects.SharedMoneyValue;
import jakarta.persistence.*;

/**
 * Domain entity representing an item in a return request.
 */
@Entity
@Table(name = "return_items")
public class DomainReturnItemEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "return_item_id")
    private Long returnItemId;

    @Column(name = "order_item_id", nullable = false, length = 50)
    private String orderItemId;

    @Column(name = "product_id", nullable = false, length = 50)
    private String productId;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "value", column = @Column(name = "return_price_amount")),
        @AttributeOverride(name = "currency", column = @Column(name = "return_price_currency"))
    })
    private SharedMoneyValue returnPrice;

    @Column(name = "reason", length = 500)
    private String reason;

    @Column(name = "condition_notes", columnDefinition = "TEXT")
    private String conditionNotes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "return_id", nullable = false)
    private DomainReturnRequestEntity returnRequest;

    // Default constructor for JPA
    protected DomainReturnItemEntity() {}

    // Constructor
    public DomainReturnItemEntity(String orderItemId, String productId, Integer quantity, 
                                 SharedMoneyValue returnPrice, String reason) {
        this.orderItemId = orderItemId;
        this.productId = productId;
        this.quantity = quantity;
        this.returnPrice = returnPrice;
        this.reason = reason;
    }

    // Getters
    public Long getReturnItemId() {
        return returnItemId;
    }

    public String getOrderItemId() {
        return orderItemId;
    }

    public String getProductId() {
        return productId;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public SharedMoneyValue getReturnPrice() {
        return returnPrice;
    }

    public String getReason() {
        return reason;
    }

    public String getConditionNotes() {
        return conditionNotes;
    }

    public DomainReturnRequestEntity getReturnRequest() {
        return returnRequest;
    }

    // Setters
    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public void setReturnPrice(SharedMoneyValue returnPrice) {
        this.returnPrice = returnPrice;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public void setConditionNotes(String conditionNotes) {
        this.conditionNotes = conditionNotes;
    }

    public void setReturnRequest(DomainReturnRequestEntity returnRequest) {
        this.returnRequest = returnRequest;
    }
}