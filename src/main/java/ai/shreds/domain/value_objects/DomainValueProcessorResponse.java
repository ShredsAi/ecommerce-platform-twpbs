package ai.shreds.domain.value_objects;

import jakarta.persistence.Embeddable;
import jakarta.persistence.Column;

/**
 * Value object capturing processor response details for audit.
 */
@Embeddable
public class DomainValueProcessorResponse {
    
    @Column(name = "processor_id")
    private String processorId;
    
    @Column(name = "response_code")
    private String responseCode;
    
    @Column(name = "response_message")
    private String responseMessage;
    
    @Column(name = "raw_response", columnDefinition = "TEXT")
    private String rawResponse;

    // Default constructor for JPA
    protected DomainValueProcessorResponse() {}

    public DomainValueProcessorResponse(String processorId,
                                        String responseCode,
                                        String responseMessage,
                                        String rawResponse) {
        this.processorId = processorId;
        this.responseCode = responseCode;
        this.responseMessage = responseMessage;
        this.rawResponse = rawResponse;
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
}