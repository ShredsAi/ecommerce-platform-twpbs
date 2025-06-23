package ai.shreds.domain.services;

import ai.shreds.domain.entities.DomainPaymentTokenEntity;
import ai.shreds.domain.exceptions.DomainPaymentException;
import ai.shreds.domain.ports.DomainOutputPortPaymentRepository;
import ai.shreds.domain.ports.DomainOutputPortTokenVault;
import ai.shreds.domain.value_objects.DomainPaymentMethodIdValue;
import ai.shreds.domain.value_objects.DomainPaymentProcessorTypeEnum;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Domain service implementing payment method tokenization business logic.
 * Handles secure tokenization of sensitive payment data using external vault services.
 */
public class DomainTokenizationService {

    private final DomainOutputPortTokenVault tokenVault;
    private final DomainOutputPortPaymentRepository repository;
    
    // Default token expiry time (1 year)
    private static final int DEFAULT_TOKEN_EXPIRY_MONTHS = 12;

    public DomainTokenizationService(
            DomainOutputPortTokenVault tokenVault,
            DomainOutputPortPaymentRepository repository) {
        this.tokenVault = Objects.requireNonNull(tokenVault, "tokenVault cannot be null");
        this.repository = Objects.requireNonNull(repository, "repository cannot be null");
    }

    /**
     * Tokenizes card data and creates a payment token entity.
     * 
     * @param cardData the card data to tokenize (contains PCI sensitive information)
     * @param paymentMethodId the payment method ID this token belongs to
     * @param processorType the processor type for this token
     * @return a new payment token entity
     */
    public DomainPaymentTokenEntity tokenizeCard(
            Map<String, Object> cardData,
            DomainPaymentMethodIdValue paymentMethodId,
            DomainPaymentProcessorTypeEnum processorType) {
        
        Objects.requireNonNull(cardData, "cardData cannot be null");
        Objects.requireNonNull(paymentMethodId, "paymentMethodId cannot be null");
        Objects.requireNonNull(processorType, "processorType cannot be null");

        // Validate card data
        validateCardData(cardData);

        try {
            // Call external token vault to securely tokenize the card data
            String processorToken = tokenVault.tokenizeCard(cardData);

            if (processorToken == null || processorToken.trim().isEmpty()) {
                throw new DomainPaymentException(
                    "Token vault returned empty token",
                    "TOKENIZATION_FAILED"
                );
            }

            // Calculate expiry time
            LocalDateTime expiresAt = LocalDateTime.now().plusMonths(DEFAULT_TOKEN_EXPIRY_MONTHS);

            // Create payment token entity
            DomainPaymentTokenEntity tokenEntity = DomainPaymentTokenEntity.create(
                UUID.randomUUID(),
                paymentMethodId,
                processorToken,
                processorType,
                expiresAt
            );

            return tokenEntity;

        } catch (Exception e) {
            throw new DomainPaymentException(
                "Failed to tokenize card data: " + e.getMessage(),
                "TOKENIZATION_FAILED",
                e
            );
        }
    }

    /**
     * Retrieves card details using a token (for authorized operations only).
     * 
     * @param tokenId the token ID
     * @return the card details map
     */
    public Map<String, Object> retrieveCardDetails(String tokenId) {
        Objects.requireNonNull(tokenId, "tokenId cannot be null");

        try {
            return tokenVault.retrieveToken(tokenId);
        } catch (Exception e) {
            throw new DomainPaymentException(
                "Failed to retrieve token details: " + e.getMessage(),
                "TOKEN_RETRIEVAL_FAILED",
                e
            );
        }
    }

    /**
     * Validates a payment token to ensure it's still valid and not expired.
     * 
     * @param token the payment token entity
     * @return true if token is valid, false otherwise
     */
    public boolean validateToken(DomainPaymentTokenEntity token) {
        Objects.requireNonNull(token, "token cannot be null");

        // Check if token is expired
        if (token.isExpired()) {
            return false;
        }

        // Validate with external vault (optional - could be expensive)
        try {
            Map<String, Object> tokenDetails = tokenVault.retrieveToken(token.getProcessorToken());
            return tokenDetails != null && !tokenDetails.isEmpty();
        } catch (Exception e) {
            // If we can't validate with vault, consider token invalid
            return false;
        }
    }

    /**
     * Creates a token for an existing payment method by re-tokenizing.
     * This might be used for token rotation or processor migration.
     * 
     * @param existingToken the existing token to re-tokenize
     * @param newProcessorType the new processor type
     * @return a new payment token entity
     */
    public DomainPaymentTokenEntity reTokenize(
            DomainPaymentTokenEntity existingToken,
            DomainPaymentProcessorTypeEnum newProcessorType) {
        
        Objects.requireNonNull(existingToken, "existingToken cannot be null");
        Objects.requireNonNull(newProcessorType, "newProcessorType cannot be null");

        // Retrieve original card data using existing token
        Map<String, Object> cardData = retrieveCardDetails(existingToken.getProcessorToken());

        if (cardData == null || cardData.isEmpty()) {
            throw new DomainPaymentException(
                "Cannot re-tokenize: original card data not accessible",
                "RETOKENIZATION_FAILED"
            );
        }

        // Create new token with new processor type
        return tokenizeCard(cardData, existingToken.getPaymentMethodId(), newProcessorType);
    }

    /**
     * Handles cleanup of expired tokens.
     * This method should be called periodically to clean up expired tokens.
     */
    public void cleanupExpiredTokens() {
        // Note: This would require a repository method to find expired tokens
        // In a complete implementation, you'd add findExpiredTokens method to repository
        throw new UnsupportedOperationException(
            "Repository method findExpiredTokens not implemented yet"
        );
        
        /*
        List<DomainPaymentTokenEntity> expiredTokens = repository.findExpiredTokens();
        
        for (DomainPaymentTokenEntity token : expiredTokens) {
            try {
                // Optionally notify external vault to clean up
                // tokenVault.deleteToken(token.getProcessorToken());
                
                // Remove from our repository
                repository.deletePaymentToken(token);
                
            } catch (Exception e) {
                // Log error but continue with other tokens
                System.err.println("Failed to cleanup token " + token.getId() + ": " + e.getMessage());
            }
        }
        */
    }

    /**
     * Validates card data before tokenization.
     * 
     * @param cardData the card data to validate
     * @throws IllegalArgumentException if validation fails
     */
    private void validateCardData(Map<String, Object> cardData) {
        if (cardData.isEmpty()) {
            throw new IllegalArgumentException("Card data cannot be empty");
        }

        // Basic validation - in a real implementation, you'd validate:
        // - Card number format
        // - Expiry date
        // - CVV format
        // - Required fields presence
        
        if (!cardData.containsKey("card_number")) {
            throw new IllegalArgumentException("Card number is required");
        }

        if (!cardData.containsKey("exp_month") || !cardData.containsKey("exp_year")) {
            throw new IllegalArgumentException("Card expiry date is required");
        }

        // Validate card number is not obviously invalid
        Object cardNumber = cardData.get("card_number");
        if (cardNumber == null || cardNumber.toString().trim().length() < 13) {
            throw new IllegalArgumentException("Invalid card number format");
        }
    }

    /**
     * Gets token statistics for monitoring purposes.
     * 
     * @return map containing token statistics
     */
    public Map<String, Object> getTokenStatistics() {
        // This would return statistics about token usage, expiry, etc.
        // Implementation depends on repository methods being available
        throw new UnsupportedOperationException(
            "Token statistics not implemented yet"
        );
    }
}