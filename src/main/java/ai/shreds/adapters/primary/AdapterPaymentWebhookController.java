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
        
        log.info("Received refund completed webhook for refund ID: {}", refundData.refundId());
        
        try {
            // Validate webhook signature if provided
            if (signature == null || signature.trim().isEmpty()) {
                log.warn("Webhook signature missing for refund: {}", refundData.refundId());
            }
            
            // Validate refund data
            if (refundData.refundId() == null || refundData.refundId().trim().isEmpty()) {
                throw new AdapterValidationException("Refund ID is required", "refundId", refundData.refundId());
            }
            
            // Process refund completion based on the source
            if (refundData.cancellationId() != null && !refundData.cancellationId().trim().isEmpty()) {
                log.info("Processing refund completion for cancellation: {}", refundData.cancellationId());
                cancellationService.completeCancellation(refundData.cancellationId());
            } else if (refundData.returnId() != null && !refundData.returnId().trim().isEmpty()) {
                log.info("Processing refund completion for return: {}", refundData.returnId());
                // Update return status to indicate refund completed
                returnService.updateReturnStatus(refundData.returnId(), 
                    ai.shreds.shared.enums.SharedReturnStatusEnum.REFUNDED);
            } else {
                throw new AdapterValidationException(
                    "Either cancellation ID or return ID must be provided", 
                    "source", "missing");
            }
            
            log.info("Successfully processed refund completion webhook for refund: {}", refundData.refundId());
            return ResponseEntity.ok().build();
            
        } catch (AdapterValidationException ex) {
            log.error("Validation error processing refund webhook: {}", refundData.refundId(), ex);
            throw ex;
        } catch (Exception ex) {
            log.error("Unexpected error processing refund webhook: {}", refundData.refundId(), ex);
            throw new AdapterMessageProcessingException(
                "Failed to process refund completion webhook", 
                refundData.refundId(), 
                "RefundWebhook", 
                "/api/webhooks/payment/refund/completed", 
                ex);
        }
    }

    @PostMapping("/refund/failed")
    public ResponseEntity<Void> handleRefundFailed(
            @Valid @RequestBody SharedRefundRequestDTO refundData,
            @RequestHeader(value = "X-Webhook-Signature", required = false) String signature) {
        
        log.warn("Received refund failed webhook for refund ID: {}", refundData.refundId());
        
        try {
            // Validate refund data
            if (refundData.refundId() == null || refundData.refundId().trim().isEmpty()) {
                throw new AdapterValidationException("Refund ID is required", "refundId", refundData.refundId());
            }
            
            // Log the failure for manual review and potential retry
            log.error("Refund failed for ID: {}, cancellation: {}, return: {}", 
                refundData.refundId(), refundData.cancellationId(), refundData.returnId());
            
            // Here you might want to trigger compensation logic or manual review process
            // For now, we just acknowledge receipt
            
            return ResponseEntity.ok().build();
            
        } catch (Exception ex) {
            log.error("Error processing refund failure webhook: {}", refundData.refundId(), ex);
            throw new AdapterMessageProcessingException(
                "Failed to process refund failure webhook", 
                refundData.refundId(), 
                "RefundWebhook", 
                "/api/webhooks/payment/refund/failed", 
                ex);
        }
    }
}
