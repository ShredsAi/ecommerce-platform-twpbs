package ai.shreds.adapters.primary;

import org.springframework.stereotype.Component;
import org.springframework.jms.annotation.JmsListener;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import ai.shreds.application.ports.ApplicationCancellationInputPort;
import ai.shreds.application.ports.ApplicationReturnInputPort;
import ai.shreds.shared.dtos.SharedRefundRequestDTO;
import ai.shreds.shared.enums.SharedReturnStatusEnum;
import ai.shreds.adapters.exceptions.AdapterMessageProcessingException;

@Slf4j
@Component
@RequiredArgsConstructor
public class AdapterPaymentCallbackJmsListener {

    private final ApplicationCancellationInputPort cancellationService;
    private final ApplicationReturnInputPort returnService;

    @JmsListener(destination = "${spring.jms.listener.payment-callback-queue:paymentCallbackQueue}")
    public void handlePaymentCallback(SharedRefundRequestDTO refundData) {
        
        log.info("Received payment callback message for refund ID: {}", refundData.getRefundId());
        
        try {
            // Validate message
            if (refundData == null) {
                throw new AdapterMessageProcessingException("Received null payment callback message");
            }
            
            if (refundData.getRefundId() == null || refundData.getRefundId().trim().isEmpty()) {
                throw new AdapterMessageProcessingException(
                    "Refund ID is missing in payment callback message", 
                    null, 
                    "PaymentCallback"
                );
            }
            
            if (refundData.getStatus() == null || refundData.getStatus().trim().isEmpty()) {
                throw new AdapterMessageProcessingException(
                    "Status is missing in payment callback message", 
                    refundData.getRefundId(), 
                    "PaymentCallback"
                );
            }
            
            log.info("Processing payment callback for refund: {} with status: {}", 
                    refundData.getRefundId(), refundData.getStatus());
            
            // Process based on refund status
            switch (refundData.getStatus().toUpperCase()) {
                case "COMPLETED", "SUCCESS", "PROCESSED" -> {
                    handleRefundSuccess(refundData);
                }
                case "FAILED", "ERROR", "REJECTED" -> {
                    handleRefundFailure(refundData);
                }
                case "PENDING", "PROCESSING" -> {
                    log.info("Refund {} is still processing, no action needed", refundData.getRefundId());
                }
                default -> {
                    log.warn("Unknown refund status: {} for refund: {}", 
                            refundData.getStatus(), refundData.getRefundId());
                }
            }
            
            log.info("Successfully processed payment callback for refund: {}", refundData.getRefundId());
            
        } catch (AdapterMessageProcessingException ex) {
            log.error("Message processing validation error for payment callback: {}", 
                    refundData != null ? refundData.getRefundId() : "unknown", ex);
            throw ex;
        } catch (Exception ex) {
            log.error("Unexpected error processing payment callback: {}", 
                    refundData != null ? refundData.getRefundId() : "unknown", ex);
            throw new AdapterMessageProcessingException(
                "Failed to process payment callback message", 
                refundData != null ? refundData.getRefundId() : null, 
                "PaymentCallback", 
                "paymentCallbackQueue", 
                ex
            );
        }
    }
    
    private void handleRefundSuccess(SharedRefundRequestDTO refundData) {
        try {
            // Complete cancellation if this is for a cancellation
            if (refundData.getCancellationId() != null && !refundData.getCancellationId().trim().isEmpty()) {
                log.info("Completing cancellation: {} due to successful refund: {}", 
                        refundData.getCancellationId(), refundData.getRefundId());
                cancellationService.completeCancellation(refundData.getCancellationId());
            }
            
            // Update return status if this is for a return
            if (refundData.getReturnId() != null && !refundData.getReturnId().trim().isEmpty()) {
                log.info("Updating return: {} to REFUNDED due to successful refund: {}", 
                        refundData.getReturnId(), refundData.getRefundId());
                returnService.updateReturnStatus(refundData.getReturnId(), SharedReturnStatusEnum.REFUNDED);
            }
        } catch (Exception ex) {
            log.error("Error handling successful refund: {}", refundData.getRefundId(), ex);
            throw ex;
        }
    }
    
    private void handleRefundFailure(SharedRefundRequestDTO refundData) {
        log.error("Refund failed for refund ID: {}, cancellation: {}, return: {}, reason: {}", 
                refundData.getRefundId(), refundData.getCancellationId(), 
                refundData.getReturnId(), refundData.getReason());
        
        // Here you might want to:
        // 1. Trigger manual review process
        // 2. Send notification to customer service
        // 3. Schedule retry based on failure reason
        // 4. Update status to indicate failure
        
        // For now, we just log the failure for manual intervention
    }
}