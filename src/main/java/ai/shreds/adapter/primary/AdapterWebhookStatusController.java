package ai.shreds.adapter.primary;

import ai.shreds.application.dtos.ApplicationWebhookStatusDTO;
import ai.shreds.application.ports.ApplicationInputPortQueryWebhookStatus;
import ai.shreds.shared.dtos.SharedWebhookStatusResponseDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * REST controller for querying webhook processing status.
 * Provides endpoints to check the current status of webhook processing.
 */
@RestController
@RequestMapping("/webhooks/status")
@Slf4j
public class AdapterWebhookStatusController {

    private final ApplicationInputPortQueryWebhookStatus webhookStatusInputPort;

    public AdapterWebhookStatusController(ApplicationInputPortQueryWebhookStatus webhookStatusInputPort) {
        this.webhookStatusInputPort = webhookStatusInputPort;
    }

    /**
     * Retrieves the processing status of a specific webhook.
     * 
     * @param webhookId The unique identifier of the webhook
     * @return Response containing webhook status information
     */
    @GetMapping("/{webhookId}")
    public ResponseEntity<SharedWebhookStatusResponseDTO> getWebhookStatus(
            @PathVariable UUID webhookId) {
        
        log.info("Webhook status requested for ID: {}", webhookId);
        
        try {
            ApplicationWebhookStatusDTO statusDto = webhookStatusInputPort.getWebhookStatus(webhookId);
            SharedWebhookStatusResponseDTO response = statusDto.toSharedStatusResponse();
            
            log.info("Webhook status retrieved - ID: {}, Status: {}, Payment ID: {}", 
                    webhookId, response.getProcessingStatus(), response.getPaymentId());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error retrieving webhook status for ID: {}", webhookId, e);
            throw e; // Let the exception handler deal with it
        }
    }
}