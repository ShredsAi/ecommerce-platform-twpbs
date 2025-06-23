package ai.shreds.infrastructure.external_services;

import ai.shreds.infrastructure.exceptions.InfrastructureExternalServiceException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * HTTP client for Stripe API operations.
 * Handles low-level REST calls to Stripe's payment processing endpoints.
 */
@Component
public class InfrastructureStripeClient {
    
    private static final Logger log = LoggerFactory.getLogger(InfrastructureStripeClient.class);
    
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    
    @Value("${stripe.api-key}")
    private String apiKey;
    
    @Value("${stripe.api-url:https://api.stripe.com/v1}")
    private String baseUrl;
    
    public InfrastructureStripeClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
        this.objectMapper = new ObjectMapper();
    }
    
    /**
     * Create a payment intent in Stripe.
     *
     * @param request the payment intent creation request
     * @return the Stripe payment intent response
     */
    public StripePaymentIntentResponse createPaymentIntent(StripePaymentIntentRequest request) {
        try {
            log.debug("Creating Stripe payment intent for amount: {} {}", request.getAmount(), request.getCurrency());
            
            HttpHeaders headers = createHeaders();
            HttpEntity<StripePaymentIntentRequest> entity = new HttpEntity<>(request, headers);
            
            ResponseEntity<String> response = restTemplate.postForEntity(
                baseUrl + "/payment_intents",
                entity,
                String.class
            );
            
            if (response.getStatusCode().is2xxSuccessful()) {
                JsonNode responseBody = objectMapper.readTree(response.getBody());
                return mapToPaymentIntentResponse(responseBody);
            } else {
                throw InfrastructureExternalServiceException.serviceUnavailable(
                    "STRIPE",
                    "Failed to create payment intent: HTTP " + response.getStatusCode()
                );
            }
            
        } catch (RestClientException e) {
            log.error("Network error calling Stripe API: {}", e.getMessage(), e);
            throw InfrastructureExternalServiceException.networkError("STRIPE", e);
        } catch (Exception e) {
            log.error("Unexpected error calling Stripe API: {}", e.getMessage(), e);
            throw new InfrastructureExternalServiceException(
                "STRIPE",
                "API_ERROR",
                "Unexpected error: " + e.getMessage(),
                e,
                false,
                null
            );
        }
    }
    
    /**
     * Confirm a payment intent in Stripe.
     *
     * @param id the payment intent ID
     * @param request the confirmation request
     * @return the Stripe payment response
     */
    public StripePaymentResponse confirmPaymentIntent(String id, StripeConfirmRequest request) {
        try {
            log.debug("Confirming Stripe payment intent: {}", id);
            
            HttpHeaders headers = createHeaders();
            HttpEntity<StripeConfirmRequest> entity = new HttpEntity<>(request, headers);
            
            ResponseEntity<String> response = restTemplate.postForEntity(
                baseUrl + "/payment_intents/" + id + "/confirm",
                entity,
                String.class
            );
            
            if (response.getStatusCode().is2xxSuccessful()) {
                JsonNode responseBody = objectMapper.readTree(response.getBody());
                return mapToPaymentResponse(responseBody);
            } else {
                throw InfrastructureExternalServiceException.serviceUnavailable(
                    "STRIPE",
                    "Failed to confirm payment intent: HTTP " + response.getStatusCode()
                );
            }
            
        } catch (RestClientException e) {
            log.error("Network error calling Stripe API: {}", e.getMessage(), e);
            throw InfrastructureExternalServiceException.networkError("STRIPE", e);
        } catch (Exception e) {
            log.error("Unexpected error calling Stripe API: {}", e.getMessage(), e);
            throw new InfrastructureExternalServiceException(
                "STRIPE",
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
        headers.set("Authorization", "Bearer " + apiKey);
        headers.set("Stripe-Version", "2022-11-15");
        return headers;
    }
    
    private StripePaymentIntentResponse mapToPaymentIntentResponse(JsonNode response) {
        return new StripePaymentIntentResponse(
            response.get("id").asText(),
            response.get("status").asText(),
            response.get("client_secret").asText(),
            response.has("next_action") ? response.get("next_action") : null
        );
    }
    
    private StripePaymentResponse mapToPaymentResponse(JsonNode response) {
        return new StripePaymentResponse(
            response.get("id").asText(),
            response.get("status").asText(),
            response.has("charges") ? response.get("charges") : null,
            response.has("last_payment_error") ? response.get("last_payment_error") : null
        );
    }
    
    // Inner classes for request/response DTOs
    public static class StripePaymentIntentRequest {
        private Long amount;
        private String currency;
        private String paymentMethod;
        private String confirmationMethod;
        private Boolean confirm;
        private Map<String, String> metadata;
        
        // Constructors, getters, setters
        public StripePaymentIntentRequest() {}
        
        public StripePaymentIntentRequest(Long amount, String currency, String paymentMethod, 
                                        String confirmationMethod, Boolean confirm, Map<String, String> metadata) {
            this.amount = amount;
            this.currency = currency;
            this.paymentMethod = paymentMethod;
            this.confirmationMethod = confirmationMethod;
            this.confirm = confirm;
            this.metadata = metadata;
        }
        
        public Long getAmount() { return amount; }
        public void setAmount(Long amount) { this.amount = amount; }
        public String getCurrency() { return currency; }
        public void setCurrency(String currency) { this.currency = currency; }
        public String getPaymentMethod() { return paymentMethod; }
        public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }
        public String getConfirmationMethod() { return confirmationMethod; }
        public void setConfirmationMethod(String confirmationMethod) { this.confirmationMethod = confirmationMethod; }
        public Boolean getConfirm() { return confirm; }
        public void setConfirm(Boolean confirm) { this.confirm = confirm; }
        public Map<String, String> getMetadata() { return metadata; }
        public void setMetadata(Map<String, String> metadata) { this.metadata = metadata; }
    }
    
    public static class StripeConfirmRequest {
        private String paymentMethod;
        
        public StripeConfirmRequest() {}
        public StripeConfirmRequest(String paymentMethod) { this.paymentMethod = paymentMethod; }
        
        public String getPaymentMethod() { return paymentMethod; }
        public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }
    }
    
    public static class StripePaymentIntentResponse {
        private final String id;
        private final String status;
        private final String clientSecret;
        private final JsonNode nextAction;
        
        public StripePaymentIntentResponse(String id, String status, String clientSecret, JsonNode nextAction) {
            this.id = id;
            this.status = status;
            this.clientSecret = clientSecret;
            this.nextAction = nextAction;
        }
        
        public String getId() { return id; }
        public String getStatus() { return status; }
        public String getClientSecret() { return clientSecret; }
        public JsonNode getNextAction() { return nextAction; }
    }
    
    public static class StripePaymentResponse {
        private final String id;
        private final String status;
        private final JsonNode charges;
        private final JsonNode lastPaymentError;
        
        public StripePaymentResponse(String id, String status, JsonNode charges, JsonNode lastPaymentError) {
            this.id = id;
            this.status = status;
            this.charges = charges;
            this.lastPaymentError = lastPaymentError;
        }
        
        public String getId() { return id; }
        public String getStatus() { return status; }
        public JsonNode getCharges() { return charges; }
        public JsonNode getLastPaymentError() { return lastPaymentError; }
    }
}