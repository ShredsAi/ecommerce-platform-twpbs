package ai.shreds.domain.services;

import ai.shreds.domain.entities.DomainCancellationRequestEntity;
import ai.shreds.domain.ports.DomainInputPortCancellationService;
import ai.shreds.domain.ports.DomainOutputPortCancellationRepository;
import ai.shreds.domain.ports.DomainOutputPortOrderRepository;
import ai.shreds.domain.ports.DomainInputPortEligibilityChecker;
import ai.shreds.shared.enums.SharedCancellationReasonEnum;
import ai.shreds.shared.enums.SharedCancellationStatusEnum;
import ai.shreds.shared.dtos.SharedOrderSnapshotDTO;
import ai.shreds.shared.value_objects.SharedMoneyValue;
import ai.shreds.domain.exceptions.DomainCancellationNotAllowedException;
import ai.shreds.domain.exceptions.DomainBusinessRuleViolationException;

import org.springframework.stereotype.Service;
import java.util.List;
import java.util.UUID;
import java.util.Objects;

/**
 * Domain service implementing cancellation business logic.
 * Implements the DomainInputPortCancellationService and orchestrates cancellation operations.
 */
@Service
public class DomainCancellationService implements DomainInputPortCancellationService {
    
    private final DomainOutputPortCancellationRepository cancellationRepository;
    private final DomainOutputPortOrderRepository orderRepository;
    private final DomainInputPortEligibilityChecker eligibilityChecker;
    
    public DomainCancellationService(
        DomainOutputPortCancellationRepository cancellationRepository,
        DomainOutputPortOrderRepository orderRepository,
        DomainInputPortEligibilityChecker eligibilityChecker
    ) {
        this.cancellationRepository = Objects.requireNonNull(cancellationRepository, "Cancellation repository cannot be null");
        this.orderRepository = Objects.requireNonNull(orderRepository, "Order repository cannot be null");
        this.eligibilityChecker = Objects.requireNonNull(eligibilityChecker, "Eligibility checker cannot be null");
    }
    
    @Override
    public DomainCancellationRequestEntity requestCancellation(
        SharedOrderSnapshotDTO orderSnapshot, 
        SharedCancellationReasonEnum reason, 
        String notes
    ) {
        Objects.requireNonNull(orderSnapshot, "Order snapshot cannot be null");
        Objects.requireNonNull(reason, "Cancellation reason cannot be null");
        
        // Validate cancellation request
        validateCancellationRequest(orderSnapshot);
        
        // Check eligibility
        if (!eligibilityChecker.isCancellationAllowed(orderSnapshot, reason)) {
            String rejectionReason = eligibilityChecker.getCancellationRejectionReason(orderSnapshot);
            throw new DomainCancellationNotAllowedException(
                "Cancellation not allowed: " + rejectionReason, 
                orderSnapshot.getOrderId(), 
                rejectionReason
            );
        }
        
        // Check for duplicate cancellation requests
        checkDuplicateCancellation(orderSnapshot.getOrderId());
        
        // Create new cancellation request
        String cancellationId = generateCancellationId();
        DomainCancellationRequestEntity cancellation = new DomainCancellationRequestEntity(
            cancellationId,
            orderSnapshot.getOrderId(),
            orderSnapshot.getCustomerId(),
            reason,
            notes
        );
        
        // Calculate potential refund amount
        SharedMoneyValue refundAmount = calculateRefundAmount(orderSnapshot, reason);
        cancellation.setRefundAmount(refundAmount);
        
        // Save and return
        return cancellationRepository.save(cancellation);
    }
    
    @Override
    public DomainCancellationRequestEntity approveCancellation(String cancellationId) {
        Objects.requireNonNull(cancellationId, "Cancellation ID cannot be null");
        
        DomainCancellationRequestEntity cancellation = cancellationRepository.findById(cancellationId);
        if (cancellation == null) {
            throw new IllegalArgumentException("Cancellation not found: " + cancellationId);
        }
        
        // Apply business logic to approve
        cancellation.approve();
        
        return cancellationRepository.save(cancellation);
    }
    
    @Override
    public DomainCancellationRequestEntity rejectCancellation(String cancellationId, String reason) {
        Objects.requireNonNull(cancellationId, "Cancellation ID cannot be null");
        Objects.requireNonNull(reason, "Rejection reason cannot be null");
        
        DomainCancellationRequestEntity cancellation = cancellationRepository.findById(cancellationId);
        if (cancellation == null) {
            throw new IllegalArgumentException("Cancellation not found: " + cancellationId);
        }
        
        // Apply business logic to reject
        cancellation.reject(reason);
        
        return cancellationRepository.save(cancellation);
    }
    
    @Override
    public DomainCancellationRequestEntity completeCancellation(String cancellationId) {
        Objects.requireNonNull(cancellationId, "Cancellation ID cannot be null");
        
        DomainCancellationRequestEntity cancellation = cancellationRepository.findById(cancellationId);
        if (cancellation == null) {
            throw new IllegalArgumentException("Cancellation not found: " + cancellationId);
        }
        
        // Apply business logic to complete
        cancellation.complete();
        
        return cancellationRepository.save(cancellation);
    }
    
    @Override
    public DomainCancellationRequestEntity findCancellation(String cancellationId) {
        Objects.requireNonNull(cancellationId, "Cancellation ID cannot be null");
        return cancellationRepository.findById(cancellationId);
    }
    
    @Override
    public List<DomainCancellationRequestEntity> findCancellationsByOrder(String orderId) {
        Objects.requireNonNull(orderId, "Order ID cannot be null");
        return cancellationRepository.findByOrderId(orderId);
    }
    
    /**
     * Validate the cancellation request according to business rules.
     */
    private void validateCancellationRequest(SharedOrderSnapshotDTO orderSnapshot) {
        if (orderSnapshot.getOrderId() == null || orderSnapshot.getOrderId().trim().isEmpty()) {
            throw new IllegalArgumentException("Order ID cannot be null or empty");
        }
        
        if (orderSnapshot.getCustomerId() == null || orderSnapshot.getCustomerId().trim().isEmpty()) {
            throw new IllegalArgumentException("Customer ID cannot be null or empty");
        }
        
        if (orderSnapshot.getTotalAmount() == null) {
            throw new IllegalArgumentException("Order total amount cannot be null");
        }
    }
    
    /**
     * Check for duplicate cancellation requests for the same order.
     */
    private void checkDuplicateCancellation(String orderId) {
        List<DomainCancellationRequestEntity> existingCancellations = cancellationRepository.findByOrderId(orderId);
        
        boolean hasPendingOrApproved = existingCancellations.stream()
            .anyMatch(cancellation -> 
                cancellation.getStatus() == SharedCancellationStatusEnum.PENDING ||
                cancellation.getStatus() == SharedCancellationStatusEnum.APPROVED
            );
        
        if (hasPendingOrApproved) {
            throw new DomainBusinessRuleViolationException(
                "Duplicate cancellation request not allowed",
                "DUPLICATE_CANCELLATION",
                "Order " + orderId + " already has a pending or approved cancellation request"
            );
        }
    }
    
    /**
     * Calculate the refund amount based on order details and cancellation reason.
     */
    private SharedMoneyValue calculateRefundAmount(SharedOrderSnapshotDTO orderSnapshot, SharedCancellationReasonEnum reason) {
        // Business logic for refund calculation
        SharedMoneyValue totalAmount = orderSnapshot.getTotalAmount();
        
        // For seller fault or fraud, full refund including shipping
        if (reason == SharedCancellationReasonEnum.SELLER_FAULT || 
            reason == SharedCancellationReasonEnum.FRAUD_DETECTION) {
            return totalAmount;
        }
        
        // For other reasons, deduct processing fees if applicable
        // This is simplified - real implementation would be more complex
        return totalAmount;
    }
    
    /**
     * Generate a unique cancellation ID.
     */
    private String generateCancellationId() {
        return "CAN-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
    }
}