package ai.shreds.shared.value_objects;

import lombok.*;
import java.util.Map;
import ai.shreds.domain.value_objects.DomainProcessorResponseValue;

/**
 * Shared representation of processor response details.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SharedProcessorResponseValue {
    private String processorId;
    private String responseCode;
    private String responseMessage;
    private String rawResponse;
    private Map<String, Object> metadata;

    public DomainProcessorResponseValue toDomainValue() {
        return new DomainProcessorResponseValue(
                processorId,
                responseCode,
                responseMessage,
                rawResponse,
                metadata
        );
    }

    public static SharedProcessorResponseValue fromDomainValue(DomainProcessorResponseValue value) {
        return SharedProcessorResponseValue.builder()
                .processorId(value.getProcessorId())
                .responseCode(value.getResponseCode())
                .responseMessage(value.getResponseMessage())
                .rawResponse(value.getRawResponse())
                .metadata(value.getMetadata())
                .build();
    }
}