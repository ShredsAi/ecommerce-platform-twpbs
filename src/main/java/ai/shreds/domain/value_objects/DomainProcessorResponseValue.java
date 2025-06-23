package ai.shreds.domain.value_objects;

import ai.shreds.shared.value_objects.SharedProcessorResponseValue;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Value object encapsulating processor response details.
 */
public final class DomainProcessorResponseValue {
    private final String processorId;
    private final String responseCode;
    private final String responseMessage;
    private final String rawResponse;
    private final Map<String, Object> metadata;

    public DomainProcessorResponseValue(
            String processorId,
            String responseCode,
            String responseMessage,
            String rawResponse,
            Map<String, Object> metadata) {
        this.processorId = Objects.requireNonNull(processorId, "processorId cannot be null");
        this.responseCode = Objects.requireNonNull(responseCode, "responseCode cannot be null");
        this.responseMessage = Objects.requireNonNull(responseMessage, "responseMessage cannot be null");
        this.rawResponse = Objects.requireNonNull(rawResponse, "rawResponse cannot be null");
        this.metadata = metadata != null ? new HashMap<>(metadata) : new HashMap<>();
    }

    public DomainProcessorResponseValue(
            String processorId,
            String responseCode,
            String responseMessage,
            String rawResponse) {
        this(processorId, responseCode, responseMessage, rawResponse, new HashMap<>());
    }

    public String getProcessorId() {
        return processorId;
    }

    public String getResponseCode() {
        return responseCode;
    }

    public String getResponseMessage() {
        return responseMessage;
    }

    public String getRawResponse() {
        return rawResponse;
    }

    public Map<String, Object> getMetadata() {
        return new HashMap<>(metadata);
    }

    /**
     * Convert to shared DTO representation.
     */
    public SharedProcessorResponseValue toSharedValue() {
        return SharedProcessorResponseValue.fromDomainValue(this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DomainProcessorResponseValue)) return false;
        DomainProcessorResponseValue that = (DomainProcessorResponseValue) o;
        return processorId.equals(that.processorId)
                && responseCode.equals(that.responseCode)
                && responseMessage.equals(that.responseMessage)
                && rawResponse.equals(that.rawResponse)
                && metadata.equals(that.metadata);
    }

    @Override
    public int hashCode() {
        return Objects.hash(processorId, responseCode, responseMessage, rawResponse, metadata);
    }

    @Override
    public String toString() {
        return "DomainProcessorResponseValue{" +
                "processorId='" + processorId + '\'' +
                ", responseCode='" + responseCode + '\'' +
                ", responseMessage='" + responseMessage + '\'' +
                ", rawResponse='" + rawResponse + '\'' +
                ", metadata=" + metadata +
                '}';
    }
}