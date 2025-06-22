package ai.shreds.application.dtos;

import ai.shreds.domain.value_objects.DomainWebhookCommand;
import ai.shreds.shared.enums.SharedEnumPaymentProcessorType;
import lombok.Builder;
import lombok.Data;

import java.util.Map;

/**
 * DTO for incoming webhooks in the application layer.
 */
@Data
@Builder
public class ApplicationWebhookDTO {
    private String rawPayload;
    private SharedEnumPaymentProcessorType processorType;
    private String externalEventId;
    private String eventType;
    private Map<String, String> headers;

    /**
     * Convert to a domain command for processing in the domain layer.
     * Uses the correct header keys that match what SharedValueWebhookHeaders.getProcessorHeaders() returns.
     */
    public DomainWebhookCommand toDomainCommand() {
        String signature = null;
        if (headers != null) {
            switch (processorType) {
                case STRIPE:
                    signature = headers.get("Stripe-Signature");
                    break;
                case PAYPAL:
                    signature = headers.get("PayPal-Transmission-Sig");
                    break;
                case SQUARE:
                    signature = headers.get("X-Square-Signature");
                    break;
            }
        }
        return DomainWebhookCommand.builder()
                .rawPayload(rawPayload)
                .processorType(processorType)
                .externalEventId(externalEventId)
                .eventType(eventType)
                .signature(signature)
                .headers(headers)
                .build();
    }
}