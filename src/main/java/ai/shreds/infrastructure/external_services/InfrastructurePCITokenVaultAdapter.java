package ai.shreds.infrastructure.external_services;

import ai.shreds.domain.ports.DomainOutputPortTokenVault;
import ai.shreds.infrastructure.exceptions.InfrastructureExternalServiceException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class InfrastructurePCITokenVaultAdapter implements DomainOutputPortTokenVault {

    private static final Logger log = LoggerFactory.getLogger(InfrastructurePCITokenVaultAdapter.class);
    
    private final InfrastructurePCIVaultClient vaultClient;
    private final CircuitBreaker circuitBreaker;

    public InfrastructurePCITokenVaultAdapter(
            InfrastructurePCIVaultClient vaultClient,
            @Qualifier("vaultCircuitBreaker") CircuitBreaker circuitBreaker) {
        this.vaultClient = vaultClient;
        this.circuitBreaker = circuitBreaker;
    }

    @Override
    public String tokenizeCard(Map<String, Object> cardData) {
        return circuitBreaker.executeSupplier(() -> {
            try {
                log.debug("Tokenizing card data");
                
                // Extract card details
                String cardNumber = (String) cardData.get("cardNumber");
                Integer expMonth = (Integer) cardData.get("expMonth");
                Integer expYear = (Integer) cardData.get("expYear");
                String cvc = (String) cardData.get("cvc");
                String cardholderName = (String) cardData.get("cardholderName");
                
                // Validate required fields
                if (cardNumber == null || expMonth == null || expYear == null || cvc == null) {
                    throw new IllegalArgumentException("Missing required card data fields");
                }
                
                InfrastructurePCIVaultClient.TokenizationRequest request = 
                    new InfrastructurePCIVaultClient.TokenizationRequest(
                        cardNumber, expMonth, expYear, cvc, cardholderName
                    );
                
                InfrastructurePCIVaultClient.TokenizationResponse response = 
                    vaultClient.tokenize(request);
                
                log.debug("Successfully tokenized card, token expires at: {}", response.getExpiresAt());
                return response.getTokenId();
                
            } catch (IllegalArgumentException e) {
                log.error("Invalid card data: {}", e.getMessage());
                throw e;
            } catch (Exception e) {
                log.error("Error tokenizing card: {}", e.getMessage(), e);
                throw handleVaultException(e);
            }
        });
    }

    @Override
    public Map<String, Object> retrieveToken(String tokenId) {
        return circuitBreaker.executeSupplier(() -> {
            try {
                log.debug("Retrieving token details for: {}", tokenId);
                
                if (tokenId == null || tokenId.trim().isEmpty()) {
                    throw new IllegalArgumentException("Token ID cannot be null or empty");
                }
                
                InfrastructurePCIVaultClient.TokenDetailsResponse response = 
                    vaultClient.retrieve(tokenId);
                
                Map<String, Object> tokenDetails = Map.of(
                    "tokenId", response.getTokenId(),
                    "last4", response.getLast4(),
                    "brand", response.getBrand(),
                    "expMonth", response.getExpMonth(),
                    "expYear", response.getExpYear(),
                    "cardholderName", response.getCardholderName() != null ? response.getCardholderName() : "",
                    "expiresAt", response.getExpiresAt().toString(),
                    "isExpired", response.isExpired()
                );
                
                log.debug("Successfully retrieved token details");
                return tokenDetails;
                
            } catch (IllegalArgumentException e) {
                log.error("Invalid token ID: {}", e.getMessage());
                throw e;
            } catch (Exception e) {
                log.error("Error retrieving token {}: {}", tokenId, e.getMessage(), e);
                throw handleVaultException(e);
            }
        });
    }

    @Override
    public Map<String, Object> getTokenMetadata(String tokenId) {
        return circuitBreaker.executeSupplier(() -> {
            try {
                log.debug("Getting token metadata for: {}", tokenId);
                
                if (tokenId == null || tokenId.trim().isEmpty()) {
                    throw new IllegalArgumentException("Token ID cannot be null or empty");
                }
                
                InfrastructurePCIVaultClient.TokenDetailsResponse response = 
                    vaultClient.retrieve(tokenId);
                
                Map<String, Object> metadata = Map.of(
                    "tokenId", response.getTokenId(),
                    "last4", response.getLast4(),
                    "brand", response.getBrand(),
                    "expMonth", response.getExpMonth(),
                    "expYear", response.getExpYear(),
                    "expiresAt", response.getExpiresAt().toString(),
                    "isExpired", response.isExpired()
                );
                
                log.debug("Successfully retrieved token metadata for: {}", tokenId);
                return metadata;
                
            } catch (IllegalArgumentException e) {
                log.error("Invalid token ID: {}", e.getMessage());
                throw e;
            } catch (Exception e) {
                log.error("Error getting token metadata {}: {}", tokenId, e.getMessage(), e);
                throw handleVaultException(e);
            }
        });
    }

    // Added to satisfy DomainOutputPortTokenVault
    @Override
    public boolean isTokenValid(String tokenId) {
        return circuitBreaker.executeSupplier(() -> {
            try {
                Map<String, Object> details = retrieveToken(tokenId);
                Object expiredObj = details.get("isExpired");
                if (expiredObj instanceof Boolean) {
                    return !((Boolean) expiredObj);
                }
                return false;
            } catch (Exception e) {
                log.error("Error validating token {}: {}", tokenId, e.getMessage(), e);
                throw handleVaultException(e);
            }
        });
    }

    @Override
    public void deactivateToken(String tokenId) {
        log.warn("Deactivate token operation is not supported by the PCI vault client for token: {}", tokenId);
        // No-op
    }

    private InfrastructureExternalServiceException handleVaultException(Exception e) {
        boolean isRetryable = false;
        String errorCode = "UNKNOWN";
        Integer httpStatus = null;

        if (e.getMessage() != null) {
            String message = e.getMessage().toLowerCase();
            if (message.contains("timeout") || message.contains("connection")) {
                isRetryable = true;
                errorCode = "NETWORK_ERROR";
            } else if (message.contains("rate limit")) {
                isRetryable = true;
                errorCode = "RATE_LIMITED";
                httpStatus = 429;
            } else if (message.contains("unauthorized") || message.contains("forbidden")) {
                errorCode = "AUTHENTICATION_ERROR";
                httpStatus = 401;
            } else if (message.contains("not found")) {
                errorCode = "TOKEN_NOT_FOUND";
                httpStatus = 404;
            } else if (message.contains("service unavailable")) {
                isRetryable = true;
                errorCode = "SERVICE_UNAVAILABLE";
                httpStatus = 503;
            }
        }

        return new InfrastructureExternalServiceException(
            "PCI_TOKEN_VAULT",
            errorCode,
            e.getMessage(),
            e,
            isRetryable,
            httpStatus
        );
    }
}