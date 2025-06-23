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

import java.time.LocalDateTime;

/**
 * HTTP client for PCI-compliant token vault operations.
 * Handles secure tokenization and retrieval of payment card data.
 */
@Component
public class InfrastructurePCIVaultClient {
    
    private static final Logger log = LoggerFactory.getLogger(InfrastructurePCIVaultClient.class);
    
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    
    @Value("${token-vault.api-key}")
    private String apiKey;
    
    @Value("${token-vault.api-url}")
    private String baseUrl;
    
    public InfrastructurePCIVaultClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
        this.objectMapper = new ObjectMapper();
    }
    
    /**
     * Tokenize card data securely.
     *
     * @param request the tokenization request containing card data
     * @return the tokenization response with token ID
     */
    public TokenizationResponse tokenize(TokenizationRequest request) {
        try {
            log.debug("Tokenizing card data ending in: ****{}", 
                request.getCardNumber().substring(Math.max(0, request.getCardNumber().length() - 4)));
            
            HttpHeaders headers = createHeaders();
            HttpEntity<TokenizationRequest> entity = new HttpEntity<>(request, headers);
            
            ResponseEntity<String> response = restTemplate.postForEntity(
                baseUrl + "/v1/tokens",
                entity,
                String.class
            );
            
            if (response.getStatusCode().is2xxSuccessful()) {
                JsonNode responseBody = objectMapper.readTree(response.getBody());
                return mapToTokenizationResponse(responseBody);
            } else {
                throw InfrastructureExternalServiceException.serviceUnavailable(
                    "TOKEN_VAULT",
                    "Failed to tokenize card: HTTP " + response.getStatusCode()
                );
            }
            
        } catch (RestClientException e) {
            log.error("Network error calling Token Vault API: {}", e.getMessage(), e);
            throw InfrastructureExternalServiceException.networkError("TOKEN_VAULT", e);
        } catch (Exception e) {
            log.error("Unexpected error calling Token Vault API: {}", e.getMessage(), e);
            throw new InfrastructureExternalServiceException(
                "TOKEN_VAULT",
                "API_ERROR",
                "Unexpected error: " + e.getMessage(),
                e,
                false,
                null
            );
        }
    }
    
    /**
     * Retrieve token details.
     *
     * @param tokenId the token ID to retrieve
     * @return the token details response
     */
    public TokenDetailsResponse retrieve(String tokenId) {
        try {
            log.debug("Retrieving token details for: {}", tokenId);
            
            HttpHeaders headers = createHeaders();
            HttpEntity<String> entity = new HttpEntity<>(headers);
            
            ResponseEntity<String> response = restTemplate.exchange(
                baseUrl + "/v1/tokens/" + tokenId,
                HttpMethod.GET,
                entity,
                String.class
            );
            
            if (response.getStatusCode().is2xxSuccessful()) {
                JsonNode responseBody = objectMapper.readTree(response.getBody());
                return mapToTokenDetailsResponse(responseBody);
            } else if (response.getStatusCode() == HttpStatus.NOT_FOUND) {
                throw new InfrastructureExternalServiceException(
                    "TOKEN_VAULT",
                    "TOKEN_NOT_FOUND",
                    "Token not found: " + tokenId,
                    null,
                    false,
                    404
                );
            } else {
                throw InfrastructureExternalServiceException.serviceUnavailable(
                    "TOKEN_VAULT",
                    "Failed to retrieve token: HTTP " + response.getStatusCode()
                );
            }
            
        } catch (RestClientException e) {
            log.error("Network error calling Token Vault API: {}", e.getMessage(), e);
            throw InfrastructureExternalServiceException.networkError("TOKEN_VAULT", e);
        } catch (InfrastructureExternalServiceException e) {
            throw e; // Re-throw our custom exceptions
        } catch (Exception e) {
            log.error("Unexpected error calling Token Vault API: {}", e.getMessage(), e);
            throw new InfrastructureExternalServiceException(
                "TOKEN_VAULT",
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
        headers.set("X-API-Version", "2023-04-15");
        return headers;
    }
    
    private TokenizationResponse mapToTokenizationResponse(JsonNode response) {
        return new TokenizationResponse(
            response.get("tokenId").asText(),
            LocalDateTime.parse(response.get("expiresAt").asText())
        );
    }
    
    private TokenDetailsResponse mapToTokenDetailsResponse(JsonNode response) {
        return new TokenDetailsResponse(
            response.get("tokenId").asText(),
            response.get("last4").asText(),
            response.get("brand").asText(),
            response.get("expMonth").asInt(),
            response.get("expYear").asInt(),
            response.has("cardholderName") ? response.get("cardholderName").asText() : null,
            LocalDateTime.parse(response.get("expiresAt").asText()),
            response.get("isExpired").asBoolean()
        );
    }
    
    // Inner classes for request/response DTOs
    public static class TokenizationRequest {
        @JsonProperty("card_number")
        private String cardNumber;
        
        @JsonProperty("exp_month")
        private Integer expMonth;
        
        @JsonProperty("exp_year")
        private Integer expYear;
        
        private String cvc;
        
        @JsonProperty("cardholder_name")
        private String cardholderName;
        
        public TokenizationRequest() {}
        
        public TokenizationRequest(String cardNumber, Integer expMonth, Integer expYear, String cvc, String cardholderName) {
            this.cardNumber = cardNumber;
            this.expMonth = expMonth;
            this.expYear = expYear;
            this.cvc = cvc;
            this.cardholderName = cardholderName;
        }
        
        // Getters and setters
        public String getCardNumber() { return cardNumber; }
        public void setCardNumber(String cardNumber) { this.cardNumber = cardNumber; }
        public Integer getExpMonth() { return expMonth; }
        public void setExpMonth(Integer expMonth) { this.expMonth = expMonth; }
        public Integer getExpYear() { return expYear; }
        public void setExpYear(Integer expYear) { this.expYear = expYear; }
        public String getCvc() { return cvc; }
        public void setCvc(String cvc) { this.cvc = cvc; }
        public String getCardholderName() { return cardholderName; }
        public void setCardholderName(String cardholderName) { this.cardholderName = cardholderName; }
    }
    
    public static class TokenizationResponse {
        private final String tokenId;
        private final LocalDateTime expiresAt;
        
        public TokenizationResponse(String tokenId, LocalDateTime expiresAt) {
            this.tokenId = tokenId;
            this.expiresAt = expiresAt;
        }
        
        public String getTokenId() { return tokenId; }
        public LocalDateTime getExpiresAt() { return expiresAt; }
    }
    
    public static class TokenDetailsResponse {
        private final String tokenId;
        private final String last4;
        private final String brand;
        private final Integer expMonth;
        private final Integer expYear;
        private final String cardholderName;
        private final LocalDateTime expiresAt;
        private final boolean isExpired;
        
        public TokenDetailsResponse(String tokenId, String last4, String brand, Integer expMonth, 
                                  Integer expYear, String cardholderName, LocalDateTime expiresAt, boolean isExpired) {
            this.tokenId = tokenId;
            this.last4 = last4;
            this.brand = brand;
            this.expMonth = expMonth;
            this.expYear = expYear;
            this.cardholderName = cardholderName;
            this.expiresAt = expiresAt;
            this.isExpired = isExpired;
        }
        
        public String getTokenId() { return tokenId; }
        public String getLast4() { return last4; }
        public String getBrand() { return brand; }
        public Integer getExpMonth() { return expMonth; }
        public Integer getExpYear() { return expYear; }
        public String getCardholderName() { return cardholderName; }
        public LocalDateTime getExpiresAt() { return expiresAt; }
        public boolean isExpired() { return isExpired; }
    }
}