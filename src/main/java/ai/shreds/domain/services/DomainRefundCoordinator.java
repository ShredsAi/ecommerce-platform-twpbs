package ai.shreds.domain.services;

import ai.shreds.domain.entities.DomainRefundTransactionEntity;
import ai.shreds.domain.ports.DomainOutputPortRefundTransactionRepository;
import ai.shreds.shared.value_objects.SharedMoneyValue;
import ai.shreds.shared.dtos.SharedOrderSnapshotDTO;
import ai.shreds.shared.dtos.SharedOrderItemDTO;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

/**
 * Domain service for coordinating refund operations.
 * Contains business logic for calculating and managing refunds.
 */
public class DomainRefundCoordinator {
    
    private final DomainOutputPortRefundTransactionRepository refundRepository;
    
    // Business constants for fees and calculations
    private static final double STANDARD_RESTOCKING_FEE_RATE = 0.15; // 15%
    private static final double DEFECTIVE_PRODUCT_RESTOCKING_FEE_RATE = 0.0; // No fee for defective products
    private static final double SHIPPING_REFUND_THRESHOLD = 50.0; // Minimum order value for shipping refund
    
    public DomainRefundCoordinator(DomainOutputPortRefundTransactionRepository refundRepository) {
        this.refundRepository = Objects.requireNonNull(refundRepository, "Refund repository cannot be null");
    }
    
    /**
     * Initiate a refund for a cancellation.
     * 
     * @param cancellationId the ID of the cancellation
     * @param amount the refund amount
     * @return the created refund transaction entity
     */
    public DomainRefundTransactionEntity initiateRefund(String cancellationId, SharedMoneyValue amount) {
        Objects.requireNonNull(cancellationId, "Cancellation ID cannot be null");
        Objects.requireNonNull(amount, "Amount cannot be null");
        
        String refundId = generateRefundId();
        String reason = "Order cancellation";
        
        DomainRefundTransactionEntity refund = new DomainRefundTransactionEntity(
            refundId,
            cancellationId,
            amount,
            reason
        );
        
        return refundRepository.save(refund);
    }
    
    /**
     * Initiate a refund for a return.
     * 
     * @param returnId the ID of the return
     * @param amount the refund amount
     * @return the created refund transaction entity
     */
    public DomainRefundTransactionEntity initiateReturnRefund(String returnId, SharedMoneyValue amount) {
        Objects.requireNonNull(returnId, "Return ID cannot be null");
        Objects.requireNonNull(amount, "Amount cannot be null");
        
        String refundId = generateRefundId();
        String reason = "Product return";
        
        DomainRefundTransactionEntity refund = new DomainRefundTransactionEntity(
            refundId,
            returnId,
            amount,
            reason,
            true // isReturn flag
        );
        
        return refundRepository.save(refund);
    }
    
    /**
     * Calculate the refund amount for a cancellation.
     * 
     * @param orderSnapshot the order snapshot
     * @param reason the cancellation reason
     * @return the calculated refund amount
     */
    public SharedMoneyValue calculateRefundAmount(SharedOrderSnapshotDTO orderSnapshot, String reason) {
        Objects.requireNonNull(orderSnapshot, "Order snapshot cannot be null");
        Objects.requireNonNull(reason, "Reason cannot be null");
        
        SharedMoneyValue totalAmount = orderSnapshot.getTotalAmount();
        
        // For seller fault, fraud, or system issues, provide full refund
        if (isFullRefundEligible(reason)) {
            return totalAmount;
        }
        
        // For customer-initiated cancellations, may apply processing fees
        if ("CUSTOMER_REQUEST".equals(reason)) {
            // Apply minimal processing fee for customer convenience
            BigDecimal processingFee = totalAmount.amount().multiply(BigDecimal.valueOf(0.02)); // 2%
            BigDecimal refundAmount = totalAmount.amount().subtract(processingFee);
            
            if (refundAmount.compareTo(BigDecimal.ZERO) < 0) {
                refundAmount = BigDecimal.ZERO;
            }
            
            return new SharedMoneyValue(refundAmount, totalAmount.currency());
        }
        
        // Default: full refund
        return totalAmount;
    }
    
    /**
     * Apply restocking fee to a base refund amount.
     * 
     * @param baseAmount the base refund amount
     * @param reason the return reason
     * @return the refund amount after applying restocking fee
     */
    public SharedMoneyValue applyRestockingFee(SharedMoneyValue baseAmount, String reason) {
        Objects.requireNonNull(baseAmount, "Base amount cannot be null");
        Objects.requireNonNull(reason, "Reason cannot be null");
        
        double feeRate = getRestockingFeeRate(reason);
        
        if (feeRate == 0.0) {
            return baseAmount; // No fee applied
        }
        
        BigDecimal fee = baseAmount.amount().multiply(BigDecimal.valueOf(feeRate));
        BigDecimal refundAmount = baseAmount.amount().subtract(fee);
        
        if (refundAmount.compareTo(BigDecimal.ZERO) < 0) {
            refundAmount = BigDecimal.ZERO;
        }
        
        return new SharedMoneyValue(refundAmount, baseAmount.currency());
    }
    
    /**
     * Calculate refund amount for specific items in a return.
     * 
     * @param orderSnapshot the original order
     * @param returnedItems the items being returned
     * @param returnReason the reason for return
     * @return the calculated refund amount
     */
    public SharedMoneyValue calculateItemRefundAmount(
        SharedOrderSnapshotDTO orderSnapshot, 
        java.util.List<String> returnedItemIds, 
        String returnReason
    ) {
        Objects.requireNonNull(orderSnapshot, "Order snapshot cannot be null");
        Objects.requireNonNull(returnedItems, "Returned items cannot be null");
        Objects.requireNonNull(returnReason, "Return reason cannot be null");
        
        BigDecimal totalRefund = BigDecimal.ZERO;
        String currency = orderSnapshot.getTotalAmount().currency();
        
        for (SharedOrderItemDTO item : orderSnapshot.getItems()) {
            if (returnedItemIds.contains(item.getOrderItemId())) {
                totalRefund = totalRefund.add(item.getTotalPrice().amount());
            }
        }
        
        SharedMoneyValue baseRefund = new SharedMoneyValue(totalRefund, currency);
        
        // Apply restocking fee if applicable
        return applyRestockingFee(baseRefund, returnReason);
    }
    
    /**
     * Determine if a cancellation reason qualifies for full refund.
     */
    private boolean isFullRefundEligible(String reason) {
        return "SELLER_FAULT".equals(reason) ||
               "FRAUD_DETECTION".equals(reason) ||
               "INVENTORY_FAILURE".equals(reason) ||
               "PAYMENT_FAILURE".equals(reason);
    }
    
    /**
     * Get the restocking fee rate based on return reason.
     */
    private double getRestockingFeeRate(String reason) {
        switch (reason) {
            case "DEFECTIVE_PRODUCT":
            case "WRONG_ITEM":
            case "NOT_AS_DESCRIBED":
            case "DAMAGED_IN_SHIPPING":
                return DEFECTIVE_PRODUCT_RESTOCKING_FEE_RATE; // No fee for seller issues
            
            case "CHANGED_MIND":
            case "SIZE_ISSUE":
                return STANDARD_RESTOCKING_FEE_RATE; // Standard fee for customer reasons
            
            default:
                return STANDARD_RESTOCKING_FEE_RATE; // Default fee
        }
    }
    
    /**
     * Generate a unique refund ID.
     */
    private String generateRefundId() {
        return "REF-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
    }
}