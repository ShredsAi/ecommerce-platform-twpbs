package ai.shreds.domain.ports;

import java.util.Map;

/**
 * Output port for token vault operations.
 * This interface defines the contract for interacting with PCI-compliant token vault services.
 */
public interface DomainOutputPortTokenVault {

    /**
     * Tokenizes card data using a PCI-compliant vault.
     * @param cardData the sensitive card data to tokenize
     * @return the secure token representing the card data
     */
    String tokenizeCard(Map<String, Object> cardData);

    /**
     * Retrieves the original card data using a token.
     * @param tokenId the token to retrieve card data for
     * @return the card data associated with the token
     */
    Map<String, Object> retrieveToken(String tokenId);

    /**
     * Validates if a token is still valid and active.
     * @param tokenId the token to validate
     * @return true if the token is valid, false otherwise
     */
    boolean isTokenValid(String tokenId);

    /**
     * Deactivates a token, making it unusable for future operations.
     * @param tokenId the token to deactivate
     */
    void deactivateToken(String tokenId);

    /**
     * Gets metadata about a token without retrieving sensitive data.
     * @param tokenId the token to get metadata for
     * @return metadata about the token (e.g., expiry date, card type)
     */
    Map<String, Object> getTokenMetadata(String tokenId);
}