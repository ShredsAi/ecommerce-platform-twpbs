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
 * HTTP client for 3D Secure authentication service.
 * Handles authentication flows for payment card security.
 */
@Component
public class InfrastructureThreeDSecureClient {
    
    private static final Logger log = LoggerFactory.getLogger(InfrastructureThreeDSecureClient.class);
    
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    
    @Value("${threeds.api-key}")
    private String apiKey;
    
    @Value("${threeds.api-url}")
    private String baseUrl;
    
    public InfrastructureThreeDSecureClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
        this.objectMapper = new ObjectMapper();
    }
    
    /**
     * Initiate 3D Secure authentication process.
     *
     * @param request the authentication request
     * @return the authentication response with challenge URL if needed
     */
    public AuthenticationResponse authenticate(AuthenticationRequest request) {
        try {
            log.debug("Initiating 3D Secure authentication for payment intent: {}", request.getPaymentIntentId());
            
            HttpHeaders headers = createHeaders();
            HttpEntity<AuthenticationRequest> entity = new HttpEntity<>(request, headers);
            
            ResponseEntity<String> response = restTemplate.postForEntity(
                baseUrl + "/v1/authentication",
                entity,
                String.class
            );
            
            if (response.getStatusCode().is2xxSuccessful()) {
                JsonNode responseBody = objectMapper.readTree(response.getBody());
                return mapToAuthenticationResponse(responseBody);
            } else {
                throw InfrastructureExternalServiceException.serviceUnavailable(
                    "3DS_PROVIDER",
                    "Failed to initiate 3D Secure authentication: HTTP " + response.getStatusCode()
                );
            }
            
        } catch (RestClientException e) {
            log.error("Network error calling 3DS Provider API: {}", e.getMessage(), e);
            throw InfrastructureExternalServiceException.networkError("3DS_PROVIDER", e);
        } catch (Exception e) {
            log.error("Unexpected error calling 3DS Provider API: {}", e.getMessage(), e);
            throw new InfrastructureExternalServiceException(
                "3DS_PROVIDER",
                "API_ERROR",
                "Unexpected error: " + e.getMessage(),
                e,
                false,
                null
            );
        }
    }
    
    /**
     * Verify 3D Secure authentication result.
     *
     * @param authId the authentication ID to verify
     * @return the verification response with authentication data
     */
    public VerificationResponse verify(String authId) {
        try {
            log.debug("Verifying 3D Secure authentication: {}", authId);
            
            HttpHeaders headers = createHeaders();
            HttpEntity<String> entity = new HttpEntity<>(headers);
            
            ResponseEntity<String> response = restTemplate.exchange(
                baseUrl + "/v1/authentication/" + authId + "/verify",
                HttpMethod.POST,
                entity,
                String.class
            );
            
            if (response.getStatusCode().is2xxSuccessful()) {
                JsonNode responseBody = objectMapper.readTree(response.getBody());
                return mapToVerificationResponse(responseBody);
            } else if (response.getStatusCode() == HttpStatus.NOT_FOUND) {
                throw new InfrastructureExternalServiceException(
                    "3DS_PROVIDER",
                    "AUTH_SESSION_NOT_FOUND",
                    "Authentication session not found: " + authId,
                    null,
                    false,
                    404
                );
            } else {
                throw InfrastructureExternalServiceException.serviceUnavailable(
                    "3DS_PROVIDER",
                    "Failed to verify 3D Secure authentication: HTTP " + response.getStatusCode()
                );
            }
            
        } catch (RestClientException e) {
            log.error("Network error calling 3DS Provider API: {}", e.getMessage(), e);
            throw InfrastructureExternalServiceException.networkError("3DS_PROVIDER", e);
        } catch (InfrastructureExternalServiceException e) {
            throw e; // Re-throw our custom exceptions
        } catch (Exception e) {
            log.error("Unexpected error calling 3DS Provider API: {}", e.getMessage(), e);
            throw new InfrastructureExternalServiceException(
                "3DS_PROVIDER",
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
        headers.set("X-API-Version", "2.2.0");
        return headers;
    }
    
    private AuthenticationResponse mapToAuthenticationResponse(JsonNode response) {
        return new AuthenticationResponse(
            response.get("authId").asText(),
            response.get("status").asText(),
            response.get("transactionId").asText(),
            response.has("challengeUrl") ? response.get("challengeUrl").asText() : null,
            response.has("acsUrl") ? response.get("acsUrl").asText() : null,
            response.has("paReq") ? response.get("paReq").asText() : null,
            response.has("merchantData") ? response.get("merchantData").asText() : null,
            response.has("threeDSVersion") ? response.get("threeDSVersion").asText() : "2.0.0"
        );
    }
    
    private VerificationResponse mapToVerificationResponse(JsonNode response) {
        return new VerificationResponse(
            response.get("authId").asText(),
            response.get("status").asText(),
            response.get("transactionId").asText(),
            response.has("eci") ? response.get("eci").asText() : null,
            response.has("cavv") ? response.get("cavv").asText() : null,
            response.has("xid") ? response.get("xid").asText() : null,
            response.has("dsTransId") ? response.get("dsTransId").asText() : null,
            response.has("threeDSVersion") ? response.get("threeDSVersion").asText() : "2.0.0"
        );
    }
    
    // Inner classes for request/response DTOs
    public static class AuthenticationRequest {
        @JsonProperty("payment_intent_id")
        private String paymentIntentId;
        
        @JsonProperty("card_token")
        private String cardToken;
        
        private Long amount;
        private String currency;
        
        @JsonProperty("customer_id")
        private String customerId;
        
        @JsonProperty("return_url")
        private String returnUrl;
        
        @JsonProperty("browser_info")
        private BrowserInfo browserInfo;
        
        public AuthenticationRequest() {}
        
        public AuthenticationRequest(String paymentIntentId, String cardToken, Long amount, String currency, String customerId) {
            this.paymentIntentId = paymentIntentId;
            this.cardToken = cardToken;
            this.amount = amount;
            this.currency = currency;
            this.customerId = customerId;
        }
        
        // Getters and setters
        public String getPaymentIntentId() { return paymentIntentId; }
        public void setPaymentIntentId(String paymentIntentId) { this.paymentIntentId = paymentIntentId; }
        public String getCardToken() { return cardToken; }
        public void setCardToken(String cardToken) { this.cardToken = cardToken; }
        public Long getAmount() { return amount; }
        public void setAmount(Long amount) { this.amount = amount; }
        public String getCurrency() { return currency; }
        public void setCurrency(String currency) { this.currency = currency; }
        public String getCustomerId() { return customerId; }
        public void setCustomerId(String customerId) { this.customerId = customerId; }
        public String getReturnUrl() { return returnUrl; }
        public void setReturnUrl(String returnUrl) { this.returnUrl = returnUrl; }
        public BrowserInfo getBrowserInfo() { return browserInfo; }
        public void setBrowserInfo(BrowserInfo browserInfo) { this.browserInfo = browserInfo; }
        
        public static class BrowserInfo {
            @JsonProperty("accept_header")
            private String acceptHeader;
            
            @JsonProperty("browser_language")
            private String browserLanguage;
            
            @JsonProperty("screen_height")
            private Integer screenHeight;
            
            @JsonProperty("screen_width")
            private Integer screenWidth;
            
            @JsonProperty("user_agent")
            private String userAgent;
            
            @JsonProperty("time_zone")
            private Integer timeZone;
            
            @JsonProperty("java_enabled")
            private Boolean javaEnabled;
            
            @JsonProperty("javascript_enabled")
            private Boolean javascriptEnabled;
            
            // Getters and setters
            public String getAcceptHeader() { return acceptHeader; }
            public void setAcceptHeader(String acceptHeader) { this.acceptHeader = acceptHeader; }
            public String getBrowserLanguage() { return browserLanguage; }
            public void setBrowserLanguage(String browserLanguage) { this.browserLanguage = browserLanguage; }
            public Integer getScreenHeight() { return screenHeight; }
            public void setScreenHeight(Integer screenHeight) { this.screenHeight = screenHeight; }
            public Integer getScreenWidth() { return screenWidth; }
            public void setScreenWidth(Integer screenWidth) { this.screenWidth = screenWidth; }
            public String getUserAgent() { return userAgent; }
            public void setUserAgent(String userAgent) { this.userAgent = userAgent; }
            public Integer getTimeZone() { return timeZone; }
            public void setTimeZone(Integer timeZone) { this.timeZone = timeZone; }
            public Boolean getJavaEnabled() { return javaEnabled; }
            public void setJavaEnabled(Boolean javaEnabled) { this.javaEnabled = javaEnabled; }
            public Boolean getJavascriptEnabled() { return javascriptEnabled; }
            public void setJavascriptEnabled(Boolean javascriptEnabled) { this.javascriptEnabled = javascriptEnabled; }
        }
    }
    
    public static class AuthenticationResponse {
        private final String authId;
        private final String status;
        private final String transactionId;
        private final String challengeUrl;
        private final String acsUrl;
        private final String paReq;
        private final String merchantData;
        private final String threeDSVersion;
        
        public AuthenticationResponse(String authId, String status, String transactionId, String challengeUrl, 
                                   String acsUrl, String paReq, String merchantData, String threeDSVersion) {
            this.authId = authId;
            this.status = status;
            this.transactionId = transactionId;
            this.challengeUrl = challengeUrl;
            this.acsUrl = acsUrl;
            this.paReq = paReq;
            this.merchantData = merchantData;
            this.threeDSVersion = threeDSVersion;
        }
        
        public String getAuthId() { return authId; }
        public String getStatus() { return status; }
        public String getTransactionId() { return transactionId; }
        public String getChallengeUrl() { return challengeUrl; }
        public String getAcsUrl() { return acsUrl; }
        public String getPaReq() { return paReq; }
        public String getMerchantData() { return merchantData; }
        public String getThreeDSVersion() { return threeDSVersion; }
    }
    
    public static class VerificationResponse {
        private final String authId;
        private final String status;
        private final String transactionId;
        private final String eci;
        private final String cavv;
        private final String xid;
        private final String dsTransId;
        private final String threeDSVersion;
        
        public VerificationResponse(String authId, String status, String transactionId, String eci, 
                                 String cavv, String xid, String dsTransId, String threeDSVersion) {
            this.authId = authId;
            this.status = status;
            this.transactionId = transactionId;
            this.eci = eci;
            this.cavv = cavv;
            this.xid = xid;
            this.dsTransId = dsTransId;
            this.threeDSVersion = threeDSVersion;
        }
        
        public String getAuthId() { return authId; }
        public String getStatus() { return status; }
        public String getTransactionId() { return transactionId; }
        public String getEci() { return eci; }
        public String getCavv() { return cavv; }
        public String getXid() { return xid; }
        public String getDsTransId() { return dsTransId; }
        public String getThreeDSVersion() { return threeDSVersion; }
    }
}