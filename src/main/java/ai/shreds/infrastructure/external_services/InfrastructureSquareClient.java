package ai.shreds.infrastructure.external_services;

import ai.shreds.infrastructure.exceptions.InfrastructureExternalServiceException;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

/**
 * HTTP client for Square API operations.
 * Handles REST calls to Square's payment processing endpoints.
 */
@Component
public class InfrastructureSquareClient {
    
    private static final Logger log = LoggerFactory.getLogger(InfrastructureSquareClient.class);
    
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    
    @Value("${square.access-token}")
    private String accessToken;
    
    @Value("${square.api-url:https://connect.squareup.com}")
    private String baseUrl;
    
    @Value("${square.application-id:payment-processing-app}")
    private String applicationId;
    
    public InfrastructureSquareClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
        this.objectMapper = new ObjectMapper();
    }
    
    /**
     * Create a payment in Square.
     *
     * @param request the payment creation request
     * @return the Square payment response
     */
    public SquarePaymentResponse createPayment(SquarePaymentRequest request) {
        try {
            log.debug("Creating Square payment for amount: {} {}", request.getAmountMoney().getAmount(), request.getAmountMoney().getCurrency());
            
            HttpHeaders headers = createHeaders();
            HttpEntity<SquarePaymentRequest> entity = new HttpEntity<>(request, headers);
            
            ResponseEntity<String> response = restTemplate.postForEntity(
                baseUrl + "/v2/payments",
                entity,
                String.class
            );
            
            if (response.getStatusCode().is2xxSuccessful()) {
                JsonNode responseBody = objectMapper.readTree(response.getBody());
                return mapToPaymentResponse(responseBody);
            } else {
                throw InfrastructureExternalServiceException.serviceUnavailable(
                    "SQUARE",
                    "Failed to create payment: HTTP " + response.getStatusCode()
                );
            }
            
        } catch (RestClientException e) {
            log.error("Network error calling Square API: {}", e.getMessage(), e);
            throw InfrastructureExternalServiceException.networkError("SQUARE", e);
        } catch (Exception e) {
            log.error("Unexpected error calling Square API: {}", e.getMessage(), e);
            throw new InfrastructureExternalServiceException(
                "SQUARE",
                "API_ERROR",
                "Unexpected error: " + e.getMessage(),
                e,
                false,
                null
            );
        }
    }
    
    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + accessToken);
        headers.set("Square-Version", "2022-11-16");
        return headers;
    }
    
    private SquarePaymentResponse mapToPaymentResponse(JsonNode response) {
        JsonNode payment = response.get("payment");
        return new SquarePaymentResponse(
            payment.get("id").asText(),
            payment.get("status").asText(),
            payment.has("amount_money") ? payment.get("amount_money") : null,
            payment.has("receipt_number") ? payment.get("receipt_number").asText() : null
        );
    }
    
    // Inner classes for request/response DTOs
    public static class SquarePaymentRequest {
        @JsonProperty("source_id")
        private String sourceId;
        
        @JsonProperty("idempotency_key")
        private String idempotencyKey;
        
        @JsonProperty("amount_money")
        private AmountMoney amountMoney;
        
        @JsonProperty("app_fee_money")
        private AmountMoney appFeeMoney;
        
        @JsonProperty("delay_duration")
        private String delayDuration;
        
        @JsonProperty("autocomplete")
        private Boolean autocomplete;
        
        @JsonProperty("order_id")
        private String orderId;
        
        @JsonProperty("customer_id")
        private String customerId;
        
        @JsonProperty("location_id")
        private String locationId;
        
        @JsonProperty("reference_id")
        private String referenceId;
        
        @JsonProperty("note")
        private String note;
        
        public SquarePaymentRequest() {}
        
        public SquarePaymentRequest(String sourceId, String idempotencyKey, Long amount, String currency) {
            this.sourceId = sourceId;
            this.idempotencyKey = idempotencyKey;
            this.amountMoney = new AmountMoney(amount, currency);
            this.autocomplete = true;
        }
        
        // Getters and setters
        public String getSourceId() { return sourceId; }
        public void setSourceId(String sourceId) { this.sourceId = sourceId; }
        public String getIdempotencyKey() { return idempotencyKey; }
        public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }
        public AmountMoney getAmountMoney() { return amountMoney; }
        public void setAmountMoney(AmountMoney amountMoney) { this.amountMoney = amountMoney; }
        public AmountMoney getAppFeeMoney() { return appFeeMoney; }
        public void setAppFeeMoney(AmountMoney appFeeMoney) { this.appFeeMoney = appFeeMoney; }
        public String getDelayDuration() { return delayDuration; }
        public void setDelayDuration(String delayDuration) { this.delayDuration = delayDuration; }
        public Boolean getAutocomplete() { return autocomplete; }
        public void setAutocomplete(Boolean autocomplete) { this.autocomplete = autocomplete; }
        public String getOrderId() { return orderId; }
        public void setOrderId(String orderId) { this.orderId = orderId; }
        public String getCustomerId() { return customerId; }
        public void setCustomerId(String customerId) { this.customerId = customerId; }
        public String getLocationId() { return locationId; }
        public void setLocationId(String locationId) { this.locationId = locationId; }
        public String getReferenceId() { return referenceId; }
        public void setReferenceId(String referenceId) { this.referenceId = referenceId; }
        public String getNote() { return note; }
        public void setNote(String note) { this.note = note; }
        
        public static class AmountMoney {
            private Long amount;
            private String currency;
            
            public AmountMoney() {}
            public AmountMoney(Long amount, String currency) {
                this.amount = amount;
                this.currency = currency;
            }
            
            public Long getAmount() { return amount; }
            public void setAmount(Long amount) { this.amount = amount; }
            public String getCurrency() { return currency; }
            public void setCurrency(String currency) { this.currency = currency; }
        }
    }
    
    public static class SquarePaymentResponse {
        private final String id;
        private final String status;
        private final JsonNode amountMoney;
        private final String receiptNumber;
        
        public SquarePaymentResponse(String id, String status, JsonNode amountMoney, String receiptNumber) {
            this.id = id;
            this.status = status;
            this.amountMoney = amountMoney;
            this.receiptNumber = receiptNumber;
        }
        
        public String getId() { return id; }
        public String getStatus() { return status; }
        public JsonNode getAmountMoney() { return amountMoney; }
        public String getReceiptNumber() { return receiptNumber; }
        
        @Override
        public String toString() {
            return String.format("SquarePaymentResponse{id='%s', status='%s', receiptNumber='%s'}", 
                id, status, receiptNumber);
        }
    }
}