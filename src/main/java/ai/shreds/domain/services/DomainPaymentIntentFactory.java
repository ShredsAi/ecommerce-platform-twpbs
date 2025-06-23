package ai.shreds.domain.services;

import ai.shreds.domain.commands.DomainCreateIntentCommand;
import ai.shreds.domain.entities.DomainPaymentIntentEntity;
import ai.shreds.domain.value_objects.*;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Objects;
import java.util.UUID;

/**
 * Factory for creating payment intent domain entities.
 * Ensures proper initialization and business rule enforcement.
 */
@Service
public class DomainPaymentIntentFactory {
    
    private static final int CLIENT_SECRET_LENGTH = 32;
    private static final int INTENT_EXPIRY_MINUTES = 30;
    private final SecureRandom secureRandom;
    
    public DomainPaymentIntentFactory() {
        this.secureRandom = new SecureRandom();
    }
    
    /**
     * Creates a new payment intent from the given command.
     * 
     * @param command the create intent command
     * @return a new payment intent entity
     */
    public DomainPaymentIntentEntity createIntent(DomainCreateIntentCommand command) {
        Objects.requireNonNull(command, "command cannot be null");
        
        // Generate unique ID
        DomainPaymentIntentIdValue id = new DomainPaymentIntentIdValue(UUID.randomUUID());
        
        // Generate secure client secret
        String clientSecret = generateClientSecret();
        
        // Calculate expiry time
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(INTENT_EXPIRY_MINUTES);
        
        // Determine processor type based on payment method or business rules
        DomainPaymentProcessorTypeEnum processorType = determineProcessorType(command);
        
        // Determine initial status based on whether payment method is provided
        DomainPaymentStatusEnum initialStatus = command.getPaymentMethodId() != null ? 
            DomainPaymentStatusEnum.REQUIRES_CONFIRMATION : 
            DomainPaymentStatusEnum.REQUIRES_PAYMENT_METHOD;
        
        LocalDateTime now = LocalDateTime.now();
        
        // Create the entity using the reconstruct method to set proper status
        return DomainPaymentIntentEntity.reconstruct(
                id,
                command.getOrderId(),
                command.getCustomerId(),
                command.getAmount(),
                initialStatus,
                command.getPaymentMethodId(),
                processorType,
                clientSecret,
                expiresAt,
                now,
                now,
                0L
        );
    }
    
    /**
     * Generates a cryptographically secure client secret.
     * 
     * @return a secure client secret string
     */
    private String generateClientSecret() {
        byte[] randomBytes = new byte[CLIENT_SECRET_LENGTH];
        secureRandom.nextBytes(randomBytes);
        String secret = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
        return "pi_" + secret;
    }
    
    /**
     * Determines the appropriate processor type based on business rules.
     * In a real implementation, this would consider factors like:
     * - Payment method type
     * - Customer preferences
     * - Geographic location
     * - Amount thresholds
     * - Processor availability
     * 
     * @param command the create intent command
     * @return the selected processor type
     */
    private DomainPaymentProcessorTypeEnum determineProcessorType(DomainCreateIntentCommand command) {
        // Simple logic for now - could be enhanced with more sophisticated routing
        
        // For demonstration, use different processors based on amount
        if (command.getAmount().getAmount().compareTo(java.math.BigDecimal.valueOf(1000)) > 0) {
            // High-value payments go to Stripe for better fraud protection
            return DomainPaymentProcessorTypeEnum.STRIPE;
        } else if ("USD".equals(command.getAmount().getCurrency()) || 
                   "EUR".equals(command.getAmount().getCurrency())) {
            // USD/EUR payments prefer PayPal for better international support
            return DomainPaymentProcessorTypeEnum.PAYPAL;
        } else {
            // Default to Square for other currencies
            return DomainPaymentProcessorTypeEnum.SQUARE;
        }
    }
    
    /**
     * Creates a payment intent for order processing scenarios.
     * This is a convenience method for when intents are auto-generated from orders.
     * 
     * @param orderId the order ID
     * @param customerId the customer ID
     * @param amount the payment amount
     * @param paymentMethodId the payment method ID
     * @return a new payment intent entity
     */
    public DomainPaymentIntentEntity createIntentForOrder(
            DomainOrderIdValue orderId,
            DomainCustomerIdValue customerId,
            DomainMoneyValue amount,
            DomainPaymentMethodIdValue paymentMethodId) {
        
        DomainCreateIntentCommand command = new DomainCreateIntentCommand(
                orderId,
                customerId,
                amount,
                paymentMethodId
        );
        
        return createIntent(command);
    }
    
    /**
     * Validates that the factory can create intents with the given parameters.
     * 
     * @param command the command to validate
     * @throws IllegalArgumentException if validation fails
     */
    public void validateCreateParameters(DomainCreateIntentCommand command) {
        Objects.requireNonNull(command, "command cannot be null");
        
        if (command.getOrderId() == null) {
            throw new IllegalArgumentException("Order ID is required");
        }
        
        if (command.getCustomerId() == null) {
            throw new IllegalArgumentException("Customer ID is required");
        }
        
        if (command.getAmount() == null) {
            throw new IllegalArgumentException("Amount is required");
        }
        
        if (!command.getAmount().isPositive()) {
            throw new IllegalArgumentException("Amount must be positive");
        }
        
        // Additional business rule validations could be added here
        validateAmountLimits(command.getAmount());
        validateCurrencySupport(command.getAmount().getCurrency());
    }
    
    private void validateAmountLimits(DomainMoneyValue amount) {
        // Example: minimum amount validation
        if (amount.getAmount().compareTo(java.math.BigDecimal.valueOf(0.50)) < 0) {
            throw new IllegalArgumentException("Amount must be at least 0.50 " + amount.getCurrency());
        }
        
        // Example: maximum amount validation
        if (amount.getAmount().compareTo(java.math.BigDecimal.valueOf(999999.99)) > 0) {
            throw new IllegalArgumentException("Amount exceeds maximum limit of 999,999.99 " + amount.getCurrency());
        }
    }
    
    private void validateCurrencySupport(String currency) {
        // List of supported currencies
        if (!java.util.Set.of("USD", "EUR", "GBP", "CAD", "AUD").contains(currency)) {
            throw new IllegalArgumentException("Currency " + currency + " is not supported");
        }
    }
}
