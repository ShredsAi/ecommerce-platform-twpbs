package ai.shreds.domain.value_objects;

/**
 * Enumeration representing different types of payment methods in the domain.
 */
public enum DomainPaymentMethodTypeEnum {
    /**
     * Credit or debit card payment method.
     */
    CARD,
    
    /**
     * Bank account payment method (ACH, direct debit, etc.).
     */
    BANK_ACCOUNT,
    
    /**
     * Digital wallet payment method (PayPal, Apple Pay, Google Pay, etc.).
     */
    DIGITAL_WALLET
}