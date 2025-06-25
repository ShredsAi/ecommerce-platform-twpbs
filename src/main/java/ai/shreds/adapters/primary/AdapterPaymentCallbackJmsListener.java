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
        
        log.info("Received payment callback message for refund ID: {}", refundData.refundId());
        
        try {
            // Validate message
            if (refundData == null) {
                throw new AdapterMessageProcessingException("Received null payment callback message");
            }
            
            if (refundData.refundId() == null || refundData.refundId().trim().isEmpty()) {
                throw new AdapterMessageProcessingException(
                    "Refund ID is missing in payment callback message", 
                    null, 
                    "PaymentCallback"
                );
            }
            
            if (refundData.status() == null || refundData.status().trim().isEmpty()) {
                throw new AdapterMessageProcessingException(
                    "Status is missing in payment callback message", 
                    refundData.refundId(), 
                    "PaymentCallback"
                );
            }
            
            log.info("Processing payment callback for refund: {} with status: {}", 
                    refundData.refundId(), refundData.status());
            
            // Process based on refund status
            switch (refundData.status().toUpperCase()) {
                case "COMPLETED", "SUCCESS", "PROCESSED" -> {
                    handleRefundSuccess(refundData);
                }
                case "FAILED", "ERROR", "REJECTED" -> {
                    handleRefundFailure(refundData);
                }
                case "PENDING", "PROCESSING" -> {
                    log.info("Refund {} is still processing, no action needed", refundData.refundId());
                }
                default -> {
                    log.warn("Unknown refund status: {} for refund: {}", 
                            refundData.status(), refundData.refundId());
                }
            }
            
            log.info("Successfully processed payment callback for refund: {}", refundData.refundId());
            
        } catch (AdapterMessageProcessingException ex) {
            log.error("Message processing validation error for payment callback: {}", 
                    refundData != null ? refundData.refundId() : "unknown", ex);
            throw ex;
        } catch (Exception ex) {
            log.error("Unexpected error processing payment callback: {}", 
                    refundData != null ? refundData.refundId() : "unknown", ex);
            throw new AdapterMessageProcessingException(
                "Failed to process payment callback message", 
                refundData != null ? refundData.refundId() : null, 
                "PaymentCallback", 
                "paymentCallbackQueue", 
                ex
            );
        }
    }
    
    private void handleRefundSuccess(SharedRefundRequestDTO refundData) {
        try {
            // Complete cancellation if this is for a cancellation
            if (refundData.cancellationId() != null && !refundData.cancellationId().trim().isEmpty()) {
                log.info("Completing cancellation: {} due to successful refund: {}", 
                        refundData.cancellationId(), refundData.refundId());
                cancellationService.completeCancellation(refundData.cancellationId());
            }
            
            // Update return status if this is for a return
            if (refundData.returnId() != null && !refundData.returnId().trim().isEmpty()) {
                log.info("Updating return: {} to REFUNDED due to successful refund: {}", 
                        refundData.returnId(), refundData.refundId());
                returnService.updateReturnStatus(refundData.returnId(), SharedReturnStatusEnum.REFUNDED);
            }
        } catch (Exception ex) {
            log.error("Error handling successful refund: {}", refundData.refundId(), ex);
            throw ex;
        }
    }
    
    private void handleRefundFailure(SharedRefundRequestDTO refundData) {
        log.error("Refund failed for refund ID: {}, cancellation: {}, return: {}, reason: {}", 
                refundData.refundId(), refundData.cancellationId(), 
                refundData.returnId(), refundData.reason());
        
        // Here you might want to:
        // 1. Trigger manual review process
        // 2. Send notification to customer service
        // 3. Schedule retry based on failure reason
        // 4. Update status to indicate failure
        
        // For now, we just log the failure for manual intervention
    }
}
