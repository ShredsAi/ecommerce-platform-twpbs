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
import org.springframework.util.Base64Utils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

/**
 * HTTP client for PayPal API operations.
 * Handles authentication and REST calls to PayPal's payment processing endpoints.
 */
@Component
public class InfrastructurePayPalClient {
    
    private static final Logger log = LoggerFactory.getLogger(InfrastructurePayPalClient.class);
    
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    
    @Value("${paypal.client-id}")
    private String clientId;
    
    @Value("${paypal.client-secret}")
    private String clientSecret;
    
    @Value("${paypal.api-url:https://api.paypal.com}")
    private String baseUrl;
    
    private String accessToken;
    private long tokenExpiresAt;
    
    public InfrastructurePayPalClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
        this.objectMapper = new ObjectMapper();
    }
    
    /**
     * Create an order in PayPal.
     *
     * @param request the order creation request
     * @return the PayPal order response
     */
    public PayPalOrderResponse createOrder(PayPalOrderRequest request) {
        try {
            log.debug("Creating PayPal order for amount: {} {}", request.getAmount(), request.getCurrency());
            
            ensureValidAccessToken();
            
            HttpHeaders headers = createAuthHeaders();
            HttpEntity<PayPalOrderRequest> entity = new HttpEntity<>(request, headers);
            
            ResponseEntity<String> response = restTemplate.postForEntity(
                baseUrl + "/v2/checkout/orders",
                entity,
                String.class
            );
            
            if (response.getStatusCode().is2xxSuccessful()) {
                JsonNode responseBody = objectMapper.readTree(response.getBody());
                return mapToOrderResponse(responseBody);
            } else {
                throw InfrastructureExternalServiceException.serviceUnavailable(
                    "PAYPAL",
                    "Failed to create order: HTTP " + response.getStatusCode()
                );
            }
            
        } catch (RestClientException e) {
            log.error("Network error calling PayPal API: {}", e.getMessage(), e);
            throw InfrastructureExternalServiceException.networkError("PAYPAL", e);
        } catch (Exception e) {
            log.error("Unexpected error calling PayPal API: {}", e.getMessage(), e);
            throw new InfrastructureExternalServiceException(
                "PAYPAL",
                "API_ERROR",
                "Unexpected error: " + e.getMessage(),
                e,
                false,
                null
            );
        }
    }
    
    /**
     * Capture an approved order in PayPal.
     *
     * @param orderId the order ID to capture
     * @return the PayPal capture response
     */
    public PayPalCaptureResponse captureOrder(String orderId) {
        try {
            log.debug("Capturing PayPal order: {}", orderId);
            
            ensureValidAccessToken();
            
            HttpHeaders headers = createAuthHeaders();
            HttpEntity<String> entity = new HttpEntity<>("{}", headers);
            
            ResponseEntity<String> response = restTemplate.postForEntity(
                baseUrl + "/v2/checkout/orders/" + orderId + "/capture",
                entity,
                String.class
            );
            
            if (response.getStatusCode().is2xxSuccessful()) {
                JsonNode responseBody = objectMapper.readTree(response.getBody());
                return mapToCaptureResponse(responseBody);
            } else {
                throw InfrastructureExternalServiceException.serviceUnavailable(
                    "PAYPAL",
                    "Failed to capture order: HTTP " + response.getStatusCode()
                );
            }
            
        } catch (RestClientException e) {
            log.error("Network error calling PayPal API: {}", e.getMessage(), e);
            throw InfrastructureExternalServiceException.networkError("PAYPAL", e);
        } catch (Exception e) {
            log.error("Unexpected error calling PayPal API: {}", e.getMessage(), e);
            throw new InfrastructureExternalServiceException(
                "PAYPAL",
                "API_ERROR",
                "Unexpected error: " + e.getMessage(),
                e,
                false,
                null
            );
        }
    }
    
    private void ensureValidAccessToken() {
        if (accessToken == null || System.currentTimeMillis() >= tokenExpiresAt) {
            obtainAccessToken();
        }
    }
    
    private void obtainAccessToken() {
        try {
            String credentials = Base64Utils.encodeToString((clientId + ":" + clientSecret).getBytes());
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            headers.set("Authorization", "Basic " + credentials);
            
            HttpEntity<String> entity = new HttpEntity<>("grant_type=client_credentials", headers);
            
            ResponseEntity<String> response = restTemplate.postForEntity(
                baseUrl + "/v1/oauth2/token",
                entity,
                String.class
            );
            
            if (response.getStatusCode().is2xxSuccessful()) {
                JsonNode responseBody = objectMapper.readTree(response.getBody());
                accessToken = responseBody.get("access_token").asText();
                int expiresIn = responseBody.get("expires_in").asInt();
                tokenExpiresAt = System.currentTimeMillis() + (expiresIn * 1000L) - 60000; // 1 minute buffer
            } else {
                throw InfrastructureExternalServiceException.authenticationError(
                    "PAYPAL",
                    "AUTH_FAILED",
                    "Failed to obtain access token: HTTP " + response.getStatusCode()
                );
            }
            
        } catch (Exception e) {
            throw InfrastructureExternalServiceException.authenticationError(
                "PAYPAL",
                "AUTH_ERROR",
                "Error obtaining access token: " + e.getMessage()
            );
        }
    }
    
    private HttpHeaders createAuthHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + accessToken);
        headers.set("PayPal-Request-Id", java.util.UUID.randomUUID().toString());
        return headers;
    }
    
    private PayPalOrderResponse mapToOrderResponse(JsonNode response) {
        List<PayPalLink> links = new java.util.ArrayList<>();
        if (response.has("links")) {
            for (JsonNode linkNode : response.get("links")) {
                links.add(new PayPalLink(
                    linkNode.get("href").asText(),
                    linkNode.get("rel").asText(),
                    linkNode.has("method") ? linkNode.get("method").asText() : "GET"
                ));
            }
        }
        
        return new PayPalOrderResponse(
            response.get("id").asText(),
            response.get("status").asText(),
            links
        );
    }
    
    private PayPalCaptureResponse mapToCaptureResponse(JsonNode response) {
        return new PayPalCaptureResponse(
            response.get("id").asText(),
            response.get("status").asText(),
            response.has("purchase_units") ? response.get("purchase_units") : null
        );
    }
    
    // Inner classes for request/response DTOs
    public static class PayPalOrderRequest {
        private String intent;
        @JsonProperty("purchase_units")
        private List<PurchaseUnit> purchaseUnits;
        
        public PayPalOrderRequest() {}
        
        public PayPalOrderRequest(String intent, String amount, String currency, String referenceId) {
            this.intent = intent;
            this.purchaseUnits = List.of(new PurchaseUnit(
                new Amount(amount, currency),
                referenceId
            ));
        }
        
        public String getIntent() { return intent; }
        public void setIntent(String intent) { this.intent = intent; }
        public List<PurchaseUnit> getPurchaseUnits() { return purchaseUnits; }
        public void setPurchaseUnits(List<PurchaseUnit> purchaseUnits) { this.purchaseUnits = purchaseUnits; }
        public String getAmount() { return purchaseUnits != null && !purchaseUnits.isEmpty() ? purchaseUnits.get(0).getAmount().getValue() : null; }
        public String getCurrency() { return purchaseUnits != null && !purchaseUnits.isEmpty() ? purchaseUnits.get(0).getAmount().getCurrencyCode() : null; }
        
        public static class PurchaseUnit {
            private Amount amount;
            @JsonProperty("reference_id")
            private String referenceId;
            
            public PurchaseUnit() {}
            public PurchaseUnit(Amount amount, String referenceId) {
                this.amount = amount;
                this.referenceId = referenceId;
            }
            
            public Amount getAmount() { return amount; }
            public void setAmount(Amount amount) { this.amount = amount; }
            public String getReferenceId() { return referenceId; }
            public void setReferenceId(String referenceId) { this.referenceId = referenceId; }
        }
        
        public static class Amount {
            private String value;
            @JsonProperty("currency_code")
            private String currencyCode;
            
            public Amount() {}
            public Amount(String value, String currencyCode) {
                this.value = value;
                this.currencyCode = currencyCode;
            }
            
            public String getValue() { return value; }
            public void setValue(String value) { this.value = value; }
            public String getCurrencyCode() { return currencyCode; }
            public void setCurrencyCode(String currencyCode) { this.currencyCode = currencyCode; }
        }
    }
    
    public static class PayPalOrderResponse {
        private final String id;
        private final String status;
        private final List<PayPalLink> links;
        
        public PayPalOrderResponse(String id, String status, List<PayPalLink> links) {
            this.id = id;
            this.status = status;
            this.links = links;
        }
        
        public String getId() { return id; }
        public String getStatus() { return status; }
        public List<PayPalLink> getLinks() { return links; }
        
        @Override
        public String toString() {
            return String.format("PayPalOrderResponse{id='%s', status='%s', links=%s}", id, status, links);
        }
    }
    
    public static class PayPalCaptureResponse {
        private final String id;
        private final String status;
        private final JsonNode purchaseUnits;
        
        public PayPalCaptureResponse(String id, String status, JsonNode purchaseUnits) {
            this.id = id;
            this.status = status;
            this.purchaseUnits = purchaseUnits;
        }
        
        public String getId() { return id; }
        public String getStatus() { return status; }
        public JsonNode getPurchaseUnits() { return purchaseUnits; }
    }
    
    public static class PayPalLink {
        private final String href;
        private final String rel;
        private final String method;
        
        public PayPalLink(String href, String rel, String method) {
            this.href = href;
            this.rel = rel;
            this.method = method;
        }
        
        public String getHref() { return href; }
        public String getRel() { return rel; }
        public String getMethod() { return method; }
    }
}