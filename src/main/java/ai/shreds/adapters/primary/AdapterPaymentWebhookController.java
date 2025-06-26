package ai.shreds.adapters.primary;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import ai.shreds.application.ports.ApplicationCancellationInputPort;
import ai.shreds.application.ports.ApplicationReturnInputPort;
import ai.shreds.shared.dtos.SharedRefundRequestDTO;
import ai.shreds.adapters.exceptions.AdapterValidationException;
import ai.shreds.adapters.exceptions.AdapterMessageProcessingException;

@Slf4j
@RestController
@RequestMapping("/api/webhooks/payment")
@RequiredArgsConstructor
public class AdapterPaymentWebhookController {

    private final ApplicationCancellationInputPort cancellationService;
    private final ApplicationReturnInputPort returnService;

    @PostMapping("/refund/completed")
    public ResponseEntity<Void> handleRefundCompleted(
            @Valid @RequestBody SharedRefundRequestDTO refundData,
            @RequestHeader(value = "X-Webhook-Signature", required = false) String signature) {
        
        log.info("Received refund completed webhook for refund ID: {}", refundData.getRefundId());
        
        try {
            // Validate webhook signature if provided
            if (signature == null || signature.trim().isEmpty()) {
                log.warn("Webhook signature missing for refund: {}", refundData.getRefundId());
            }
            
            // Validate refund data
            if (refundData.getRefundId() == null || refundData.getRefundId().trim().isEmpty()) {
                throw new AdapterValidationException("Refund ID is required", "refundId", refundData.getRefundId());
            }
            
            // Process refund completion based on the source
            if (refundData.getCancellationId() != null && !refundData.getCancellationId().trim().isEmpty()) {
                log.info("Processing refund completion for cancellation: {}", refundData.getCancellationId());
                cancellationService.completeCancellation(refundData.getCancellationId());
            } else if (refundData.getReturnId() != null && !refundData.getReturnId().trim().isEmpty()) {
                log.info("Processing refund completion for return: {}", refundData.getReturnId());
                // Update return status to indicate refund completed
                returnService.updateReturnStatus(refundData.getReturnId(), 
                    ai.shreds.shared.enums.SharedReturnStatusEnum.REFUNDED);
            } else {
                throw new AdapterValidationException(
                    "Either cancellation ID or return ID must be provided", 
                    "source", "missing");
            }
            
            log.info("Successfully processed refund completion webhook for refund: {}", refundData.getRefundId());
            return ResponseEntity.ok().build();
            
        } catch (AdapterValidationException ex) {
            log.error("Validation error processing refund webhook: {}", refundData.getRefundId(), ex);
            throw ex;
        } catch (Exception ex) {
            log.error("Unexpected error processing refund webhook: {}", refundData.getRefundId(), ex);
            throw new AdapterMessageProcessingException(
                "Failed to process refund completion webhook", 
                refundData.getRefundId(), 
                "RefundWebhook", 
                "/api/webhooks/payment/refund/completed", 
                ex);
        }
    }

    @PostMapping("/refund/failed")
    public ResponseEntity<Void> handleRefundFailed(
            @Valid @RequestBody SharedRefundRequestDTO refundData,
            @RequestHeader(value = "X-Webhook-Signature", required = false) String signature) {
        
        log.warn("Received refund failed webhook for refund ID: {}", refundData.getRefundId());
        
        try {
            // Validate refund data
            if (refundData.getRefundId() == null || refundData.getRefundId().trim().isEmpty()) {
                throw new AdapterValidationException("Refund ID is required", "refundId", refundData.getRefundId());
            }
            
            // Log the failure for manual review and potential retry
            log.error("Refund failed for ID: {}, cancellation: {}, return: {}", 
                refundData.getRefundId(), refundData.getCancellationId(), refundData.getReturnId());
            
            // Here you might want to trigger compensation logic or manual review process
            // For now, we just acknowledge receipt
            
            return ResponseEntity.ok().build();
            
        } catch (Exception ex) {
            log.error("Error processing refund failure webhook: {}", refundData.getRefundId(), ex);
            throw new AdapterMessageProcessingException(
                "Failed to process refund failure webhook", 
                refundData.getRefundId(), 
                "RefundWebhook", 
                "/api/webhooks/payment/refund/failed", 
                ex);
        }
    }
}