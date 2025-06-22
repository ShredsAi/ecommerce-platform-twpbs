package ai.shreds.adapter.primary;

import ai.shreds.application.dtos.ApplicationWebhookDTO;
import ai.shreds.application.dtos.ApplicationWebhookResultDTO;
import ai.shreds.application.ports.ApplicationInputPortProcessWebhook;
import ai.shreds.shared.dtos.SharedWebhookRequestDTO;
import ai.shreds.shared.dtos.SharedWebhookResponseDTO;
import ai.shreds.shared.enums.SharedEnumWebhookProcessingStatus;
import ai.shreds.shared.value_objects.SharedValueWebhookHeaders;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for handling PayPal webhook notifications.
 * Receives and processes payment events from PayPal's webhook system.
 */
@RestController
@RequestMapping("/webhooks/paypal")
@Slf4j
public class AdapterWebhookPayPalController {

    private final ApplicationInputPortProcessWebhook processWebhookInputPort;

    public AdapterWebhookPayPalController(ApplicationInputPortProcessWebhook processWebhookInputPort) {
        this.processWebhookInputPort = processWebhookInputPort;
    }

    /**
     * Processes incoming PayPal webhook notifications.
     * 
     * @param transmissionId PayPal transmission ID header
     * @param authAlgo PayPal authentication algorithm header
     * @param certUrl PayPal certificate URL header
     * @param transmissionSig PayPal transmission signature header
     * @param request The webhook request payload
     * @return Response indicating processing status
     */
    @PostMapping
    public ResponseEntity<SharedWebhookResponseDTO> processPayPalWebhook(
            @RequestHeader("PayPal-Transmission-Id") String transmissionId,
            @RequestHeader("PayPal-Auth-Algo") String authAlgo,
            @RequestHeader("PayPal-Cert-Url") String certUrl,
            @RequestHeader("PayPal-Transmission-Sig") String transmissionSig,
            @Valid @RequestBody SharedWebhookRequestDTO request) {
        
        log.info("Received PayPal webhook - Event ID: {}, Event Type: {}, Transmission ID: {}", 
                request.getExternalEventId(), request.getEventType(), transmissionId);
        
        try {
            // Build webhook headers using static factory method
            SharedValueWebhookHeaders headers = SharedValueWebhookHeaders.forPayPal(
                    transmissionId, authAlgo, certUrl, transmissionSig);

            // Convert to application DTO with headers
            ApplicationWebhookDTO appDto = request.toApplicationDTO(headers);

            // Process the webhook
            ApplicationWebhookResultDTO resultDto = processWebhookInputPort.processWebhook(appDto);
            SharedWebhookResponseDTO response = resultDto.toSharedResponse();
            
            // Return appropriate status based on processing result
            HttpStatus status = determineResponseStatus(resultDto.getStatus());
            
            log.info("PayPal webhook processed - Webhook ID: {}, Status: {}, Transmission ID: {}", 
                    response.getWebhookId(), response.getStatus(), transmissionId);
            
            return new ResponseEntity<>(response, status);
            
        } catch (Exception e) {
            log.error("Error processing PayPal webhook - Event ID: {}, Transmission ID: {}", 
                    request.getExternalEventId(), transmissionId, e);
            throw e; // Let the exception handler deal with it
        }
    }
    
    /**
     * Determines the appropriate HTTP status based on webhook processing status.
     */
    private HttpStatus determineResponseStatus(SharedEnumWebhookProcessingStatus status) {
        return switch (status) {
            case PROCESSED -> HttpStatus.OK;
            case IGNORED -> HttpStatus.OK; // Duplicate webhooks return 200
            case FAILED -> HttpStatus.INTERNAL_SERVER_ERROR;
            case PENDING -> HttpStatus.ACCEPTED; // Still processing
            default -> HttpStatus.OK;
        };
    }
}