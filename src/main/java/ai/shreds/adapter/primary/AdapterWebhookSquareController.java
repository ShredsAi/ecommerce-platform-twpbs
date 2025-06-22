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
 * REST controller for handling Square webhook notifications.
 * Receives and processes payment events from Square's webhook system.
 */
@RestController
@RequestMapping("/webhooks/square")
@Slf4j
public class AdapterWebhookSquareController {

    private final ApplicationInputPortProcessWebhook processWebhookInputPort;

    public AdapterWebhookSquareController(ApplicationInputPortProcessWebhook processWebhookInputPort) {
        this.processWebhookInputPort = processWebhookInputPort;
    }

    /**
     * Processes incoming Square webhook notifications.
     * 
     * @param squareSignature The Square signature header for verification
     * @param request The webhook request payload
     * @return Response indicating processing status
     */
    @PostMapping
    public ResponseEntity<SharedWebhookResponseDTO> processSquareWebhook(
            @RequestHeader("X-Square-Signature") String squareSignature,
            @Valid @RequestBody SharedWebhookRequestDTO request) {
        
        log.info("Received Square webhook - Event ID: {}, Event Type: {}", 
                request.getExternalEventId(), request.getEventType());
        
        try {
            // Build webhook headers using static factory method
            SharedValueWebhookHeaders headers = SharedValueWebhookHeaders.forSquare(squareSignature);

            // Convert to application DTO with headers
            ApplicationWebhookDTO appDto = request.toApplicationDTO(headers);

            // Process the webhook
            ApplicationWebhookResultDTO resultDto = processWebhookInputPort.processWebhook(appDto);
            SharedWebhookResponseDTO response = resultDto.toSharedResponse();
            
            // Return appropriate status based on processing result
            HttpStatus status = determineResponseStatus(resultDto.getStatus());
            
            log.info("Square webhook processed - Webhook ID: {}, Status: {}", 
                    response.getWebhookId(), response.getStatus());
            
            return new ResponseEntity<>(response, status);
            
        } catch (Exception e) {
            log.error("Error processing Square webhook - Event ID: {}", 
                    request.getExternalEventId(), e);
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