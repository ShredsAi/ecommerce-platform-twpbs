package ai.shreds.domain.value_objects;

import ai.shreds.shared.enums.SharedEnumPaymentProcessorType;
import lombok.Builder;

import java.util.Map;

/**
 * Command object carrying webhook data into the domain for processing.
 */
public class DomainWebhookCommand {
    private final String rawPayload;
    private final SharedEnumPaymentProcessorType processorType;
    private final String externalEventId;
    private final String eventType;
    private final String signature;
    private final Map<String, String> headers;

    @Builder
    public DomainWebhookCommand(String rawPayload,
                                SharedEnumPaymentProcessorType processorType,
                                String externalEventId,
                                String eventType,
                                String signature,
                                Map<String, String> headers) {
        this.rawPayload = rawPayload;
        this.processorType = processorType;
        this.externalEventId = externalEventId;
        this.eventType = eventType;
        this.signature = signature;
        this.headers = headers;
    }

    public String getRawPayload() {
        return rawPayload;
    }

    public SharedEnumPaymentProcessorType getProcessorType() {
        return processorType;
    }

    public String getExternalEventId() {
        return externalEventId;
    }

    public String getEventType() {
        return eventType;
    }

    public String getSignature() {
        return signature;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }
}
