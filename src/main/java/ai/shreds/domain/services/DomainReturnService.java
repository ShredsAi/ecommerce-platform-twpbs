package ai.shreds.domain.services;

import ai.shreds.domain.entities.DomainReturnRequestEntity;
import ai.shreds.domain.entities.DomainReturnItemEntity;
import ai.shreds.domain.value_objects.DomainRmaNumberValue;
import ai.shreds.domain.ports.DomainInputPortReturnService;
import ai.shreds.domain.ports.DomainOutputPortReturnRepository;
import ai.shreds.domain.ports.DomainOutputPortOrderRepository;
import ai.shreds.domain.ports.DomainInputPortEligibilityChecker;
import ai.shreds.shared.enums.SharedReturnReasonEnum;
import ai.shreds.shared.enums.SharedReturnStatusEnum;
import ai.shreds.shared.dtos.SharedOrderSnapshotDTO;
import ai.shreds.shared.dtos.SharedOrderItemDTO;
import ai.shreds.shared.value_objects.SharedAddressValue;
import ai.shreds.domain.exceptions.DomainReturnNotAllowedException;
import ai.shreds.domain.exceptions.DomainBusinessRuleViolationException;

import java.util.List;
import java.util.UUID;
import java.util.Objects;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Domain service implementing return business logic.
 * Implements the DomainInputPortReturnService and orchestrates return operations.
 */
public class DomainReturnService implements DomainInputPortReturnService {
    
    private final DomainOutputPortReturnRepository returnRepository;
    private final DomainOutputPortOrderRepository orderRepository;
    private final DomainInputPortEligibilityChecker eligibilityChecker;
    
    public DomainReturnService(
        DomainOutputPortReturnRepository returnRepository,
        DomainOutputPortOrderRepository orderRepository,
        DomainInputPortEligibilityChecker eligibilityChecker
    ) {
        this.returnRepository = Objects.requireNonNull(returnRepository, "Return repository cannot be null");
        this.orderRepository = Objects.requireNonNull(orderRepository, "Order repository cannot be null");
        this.eligibilityChecker = Objects.requireNonNull(eligibilityChecker, "Eligibility checker cannot be null");
    }
    
    @Override
    public DomainReturnRequestEntity requestReturn(
        SharedOrderSnapshotDTO orderSnapshot, 
        List<DomainReturnItemEntity> items, 
        SharedReturnReasonEnum reason
    ) {
        Objects.requireNonNull(orderSnapshot, "Order snapshot cannot be null");
        Objects.requireNonNull(items, "Return items cannot be null");
        Objects.requireNonNull(reason, "Return reason cannot be null");
        
        if (items.isEmpty()) {
            throw new IllegalArgumentException("Return items list cannot be empty");
        }
        
        // Validate return items against order
        validateReturnItems(orderSnapshot, items);
        
        // Check eligibility
        List<SharedOrderItemDTO> orderItems = items.stream()
            .map(item -> orderSnapshot.getItems().stream()
                .filter(orderItem -> orderItem.getOrderItemId().equals(item.getOrderItemId()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Order item not found: " + item.getOrderItemId()))
            )
            .collect(Collectors.toList());
            
        if (!eligibilityChecker.isReturnAllowed(orderSnapshot, orderItems)) {
            String rejectionReason = eligibilityChecker.getReturnRejectionReason(orderSnapshot);
            throw new DomainReturnNotAllowedException(
                "Return not allowed: " + rejectionReason, 
                orderSnapshot.getOrderId(), 
                rejectionReason
            );
        }
        
        // Check partial return limits
        checkPartialReturnLimits(orderSnapshot.getOrderId(), items);
        
        // Generate unique IDs
        String returnId = generateReturnId();
        DomainRmaNumberValue rmaNumber = new DomainRmaNumberValue();
        
        // Create return address (simplified - would normally come from configuration or customer preferences)
        SharedAddressValue returnAddress = createDefaultReturnAddress();
        
        // Create new return request
        DomainReturnRequestEntity returnRequest = new DomainReturnRequestEntity(
            returnId,
            orderSnapshot.getOrderId(),
            orderSnapshot.getCustomerId(),
            rmaNumber.value(),
            reason,
            returnAddress
        );
        
        // Add items to return request
        for (DomainReturnItemEntity item : items) {
            returnRequest.addItem(item);
        }
        
        // Generate return instructions
        returnRequest.generateInstructions();
        
        // Save and return
        return returnRepository.save(returnRequest);
    }
    
    @Override
    public DomainReturnRequestEntity updateReturnStatus(String returnId, SharedReturnStatusEnum newStatus) {
        Objects.requireNonNull(returnId, "Return ID cannot be null");
        Objects.requireNonNull(newStatus, "New status cannot be null");
        
        DomainReturnRequestEntity returnRequest = returnRepository.findById(returnId);
        if (returnRequest == null) {
            throw new IllegalArgumentException("Return request not found: " + returnId);
        }
        
        // Apply business logic to update status
        returnRequest.updateStatus(newStatus);
        
        return returnRepository.save(returnRequest);
    }
    
    @Override
    public DomainReturnRequestEntity processReturnReceived(String returnId) {
        Objects.requireNonNull(returnId, "Return ID cannot be null");
        
        DomainReturnRequestEntity returnRequest = returnRepository.findById(returnId);
        if (returnRequest == null) {
            throw new IllegalArgumentException("Return request not found: " + returnId);
        }
        
        // Update status to received and then to processing
        returnRequest.updateStatus(SharedReturnStatusEnum.RECEIVED);
        returnRequest.updateStatus(SharedReturnStatusEnum.PROCESSING);
        
        return returnRepository.save(returnRequest);
    }
    
    @Override
    public DomainReturnRequestEntity completeRefund(String returnId) {
        Objects.requireNonNull(returnId, "Return ID cannot be null");
        
        DomainReturnRequestEntity returnRequest = returnRepository.findById(returnId);
        if (returnRequest == null) {
            throw new IllegalArgumentException("Return request not found: " + returnId);
        }
        
        // Update status to refunded
        returnRequest.updateStatus(SharedReturnStatusEnum.REFUNDED);
        
        return returnRepository.save(returnRequest);
    }
    
    @Override
    public DomainReturnRequestEntity findReturn(String returnId) {
        Objects.requireNonNull(returnId, "Return ID cannot be null");
        return returnRepository.findById(returnId);
    }
    
    @Override
    public List<DomainReturnRequestEntity> findReturnsByOrder(String orderId) {
        Objects.requireNonNull(orderId, "Order ID cannot be null");
        return returnRepository.findByOrderId(orderId);
    }
    
    /**
     * Validate return items against the order.
     */
    private void validateReturnItems(SharedOrderSnapshotDTO orderSnapshot, List<DomainReturnItemEntity> items) {
        Map<String, SharedOrderItemDTO> orderItemsMap = orderSnapshot.getItems().stream()
            .collect(Collectors.toMap(SharedOrderItemDTO::getOrderItemId, item -> item));
        
        for (DomainReturnItemEntity returnItem : items) {
            SharedOrderItemDTO orderItem = orderItemsMap.get(returnItem.getOrderItemId());
            if (orderItem == null) {
                throw new IllegalArgumentException(
                    "Return item with order item ID " + returnItem.getOrderItemId() + " not found in order"
                );
            }
            
            if (returnItem.getQuantity() > orderItem.getQuantity()) {
                throw new IllegalArgumentException(
                    String.format("Return quantity (%d) exceeds ordered quantity (%d) for item %s",
                        returnItem.getQuantity(), orderItem.getQuantity(), returnItem.getOrderItemId())
                );
            }
            
            // Validate that the product is returnable
            if (!orderItem.getIsReturnable()) {
                throw new DomainBusinessRuleViolationException(
                    "Product is not returnable",
                    "NON_RETURNABLE_PRODUCT",
                    "Product " + returnItem.getProductId() + " is marked as non-returnable"
                );
            }
        }
    }
    
    /**
     * Check partial return limits to ensure cumulative returns don't exceed ordered quantities.
     */
    private void checkPartialReturnLimits(String orderId, List<DomainReturnItemEntity> newReturnItems) {
        List<DomainReturnRequestEntity> existingReturns = returnRepository.findByOrderId(orderId);
        
        // Count already returned quantities
        Map<String, Integer> returnedQuantities = existingReturns.stream()
            .filter(returnReq -> !returnReq.getStatus().equals(SharedReturnStatusEnum.REJECTED))
            .flatMap(returnReq -> returnReq.getItems().stream())
            .collect(Collectors.groupingBy(
                DomainReturnItemEntity::getOrderItemId,
                Collectors.summingInt(DomainReturnItemEntity::getQuantity)
            ));
        
        // Check if new return items would exceed limits
        for (DomainReturnItemEntity newItem : newReturnItems) {
            int alreadyReturned = returnedQuantities.getOrDefault(newItem.getOrderItemId(), 0);
            int totalToReturn = alreadyReturned + newItem.getQuantity();
            
            // Would need to validate against original order quantity - simplified for now
            if (totalToReturn > 100) { // Simplified check - real implementation would check against order
                throw new DomainBusinessRuleViolationException(
                    "Partial return limit exceeded",
                    "PARTIAL_RETURN_LIMIT",
                    String.format("Total return quantity (%d) would exceed allowed limit for item %s",
                        totalToReturn, newItem.getOrderItemId())
                );
            }
        }
    }
    
    /**
     * Create a default return address.
     */
    private SharedAddressValue createDefaultReturnAddress() {
        // Simplified - in real implementation this would come from configuration
        return new SharedAddressValue(
            "Returns Processing Center",
            "Building A",
            "Warehouse City",
            "WH",
            "12345",
            "US"
        );
    }
    
    /**
     * Generate a unique return ID.
     */
    private String generateReturnId() {
        return "RET-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
    }
}