package ai.shreds.shared.dtos;

import ai.shreds.application.dtos.ApplicationWebhookDTO;
import ai.shreds.shared.enums.SharedEnumPaymentProcessorType;
import ai.shreds.shared.value_objects.SharedValueWebhookHeaders;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Builder;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import java.util.Map;
import java.util.HashMap;
import java.util.Collections;

/**
 * Shared DTO representing an incoming webhook request from payment processors.
 * Contains the raw payload and metadata necessary for processing and verification.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SharedWebhookRequestDTO {
    
    @NotBlank(message = "Raw payload is required")
    private String rawPayload;
    
    @NotNull(message = "Processor type is required")
    private SharedEnumPaymentProcessorType processorType;
    
    @NotBlank(message = "External event ID is required")
    private String externalEventId;
    
    @NotBlank(message = "Event type is required")
    private String eventType;

    /**
     * Converts this shared DTO to an application-layer DTO.
     * Uses empty headers map as default - headers should be populated by the controller
     * from the HTTP request context.
     *
     * @return ApplicationWebhookDTO for use in the application layer
     */
    public ApplicationWebhookDTO toApplicationDTO() {
        return ApplicationWebhookDTO.builder()
                .rawPayload(this.rawPayload)
                .processorType(this.processorType)
                .externalEventId(this.externalEventId)
                .eventType(this.eventType)
                .headers(Collections.emptyMap())
                .build();
    }

    /**
     * Converts this shared DTO to an application-layer DTO with specific headers.
     * This method should be used when the controller has extracted webhook-specific
     * headers from the HTTP request.
     *
     * @param headers Map of HTTP headers relevant to webhook signature verification
     * @return ApplicationWebhookDTO with headers populated
     */
    public ApplicationWebhookDTO toApplicationDTO(Map<String, String> headers) {
        return ApplicationWebhookDTO.builder()
                .rawPayload(this.rawPayload)
                .processorType(this.processorType)
                .externalEventId(this.externalEventId)
                .eventType(this.eventType)
                .headers(headers != null ? new HashMap<>(headers) : Collections.emptyMap())
                .build();
    }

    /**
     * Converts this shared DTO to an application-layer DTO with webhook headers value object.
     * This method should be used when the controller has extracted and structured
     * the webhook headers into a SharedValueWebhookHeaders object.
     *
     * @param webhookHeaders Structured webhook headers for the specific processor
     * @return ApplicationWebhookDTO with headers populated from the value object
     */
    public ApplicationWebhookDTO toApplicationDTO(SharedValueWebhookHeaders webhookHeaders) {
        Map<String, String> headers = webhookHeaders != null 
                ? webhookHeaders.getProcessorHeaders(this.processorType)
                : Collections.emptyMap();
                
        return ApplicationWebhookDTO.builder()
                .rawPayload(this.rawPayload)
                .processorType(this.processorType)
                .externalEventId(this.externalEventId)
                .eventType(this.eventType)
                .headers(headers)
                .build();
    }

    /**
     * Factory method to create a SharedWebhookRequestDTO for testing purposes.
     *
     * @param processorType The payment processor type
     * @param payload       The webhook payload
     * @param eventId       The external event identifier
     * @param eventType     The event type
     * @return A new SharedWebhookRequestDTO instance
     */
    public static SharedWebhookRequestDTO forTesting(SharedEnumPaymentProcessorType processorType,
                                                     String payload,
                                                     String eventId,
                                                     String eventType) {
        return SharedWebhookRequestDTO.builder()
                .processorType(processorType)
                .rawPayload(payload)
                .externalEventId(eventId)
                .eventType(eventType)
                .build();
    }
}
