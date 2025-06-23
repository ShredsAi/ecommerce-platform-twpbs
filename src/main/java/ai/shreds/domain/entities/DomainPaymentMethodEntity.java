package ai.shreds.domain.entities;

import ai.shreds.domain.value_objects.*;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Entity representing a payment method in the domain.
 */
public class DomainPaymentMethodEntity {
    private final DomainPaymentMethodIdValue id;
    private final DomainCustomerIdValue customerId;
    private final DomainPaymentMethodTypeEnum type;
    private final DomainPaymentMethodDetailsValue details;
    private boolean isDefault;
    private boolean isActive;
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public DomainPaymentMethodEntity(
            DomainPaymentMethodIdValue id,
            DomainCustomerIdValue customerId,
            DomainPaymentMethodTypeEnum type,
            DomainPaymentMethodDetailsValue details,
            boolean isDefault,
            boolean isActive,
            LocalDateTime createdAt,
            LocalDateTime updatedAt) {
        this.id = Objects.requireNonNull(id, "id cannot be null");
        this.customerId = Objects.requireNonNull(customerId, "customerId cannot be null");
        this.type = Objects.requireNonNull(type, "type cannot be null");
        this.details = Objects.requireNonNull(details, "details cannot be null");
        this.isDefault = isDefault;
        this.isActive = isActive;
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt cannot be null");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt cannot be null");
        
        validateBusinessRules();
    }

    /**
     * Static factory method to create a new PaymentMethod.
     */
    public static DomainPaymentMethodEntity create(
            DomainPaymentMethodIdValue id,
            DomainCustomerIdValue customerId,
            DomainPaymentMethodTypeEnum type,
            DomainPaymentMethodDetailsValue details,
            boolean isDefault) {
        LocalDateTime now = LocalDateTime.now();
        return new DomainPaymentMethodEntity(
                id,
                customerId,
                type,
                details,
                isDefault,
                true, // New payment methods are active by default
                now,
                now
        );
    }

    /**
     * Activates this payment method.
     */
    public void activate() {
        if (isActive) {
            return; // Already active
        }
        
        this.isActive = true;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Deactivates this payment method.
     */
    public void deactivate() {
        if (!isActive) {
            return; // Already inactive
        }
        
        this.isActive = false;
        this.isDefault = false; // Cannot be default if inactive
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Sets this payment method as the default for the customer.
     */
    public void setAsDefault() {
        if (!isActive) {
            throw new IllegalStateException("Cannot set inactive payment method as default");
        }
        
        this.isDefault = true;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Removes the default status from this payment method.
     */
    public void removeAsDefault() {
        this.isDefault = false;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Checks if this payment method can process the given amount.
     * @param amount the amount to check
     * @return true if the payment method can process the amount
     */
    public boolean canProcessAmount(DomainMoneyValue amount) {
        Objects.requireNonNull(amount, "amount cannot be null");
        
        if (!isActive) {
            return false;
        }
        
        if (!amount.isPositive()) {
            return false;
        }
        
        // Basic validation based on payment method type
        switch (type) {
            case CARD:
                // Cards typically have higher limits
                return true;
            case BANK_ACCOUNT:
                // Bank accounts can handle larger amounts but may have daily limits
                return true;
            case DIGITAL_WALLET:
                // Digital wallets may have lower limits
                return true;
            default:
                return false;
        }
    }

    private void validateBusinessRules() {
        // Validate that the details type matches the payment method type
        if (!isDetailsTypeValid()) {
            throw new IllegalArgumentException("Payment method details type does not match payment method type");
        }
    }

    private boolean isDetailsTypeValid() {
        switch (type) {
            case CARD:
                return details.getType() == DomainPaymentMethodTypeEnum.CARD && details.getCardDetails() != null;
            case BANK_ACCOUNT:
                return details.getType() == DomainPaymentMethodTypeEnum.BANK_ACCOUNT && details.getBankAccountDetails() != null;
            case DIGITAL_WALLET:
                return details.getType() == DomainPaymentMethodTypeEnum.DIGITAL_WALLET && details.getDigitalWalletDetails() != null;
            default:
                return false;
        }
    }

    // Getters
    public DomainPaymentMethodIdValue getId() {
        return id;
    }

    public DomainCustomerIdValue getCustomerId() {
        return customerId;
    }

    public DomainPaymentMethodTypeEnum getType() {
        return type;
    }

    public DomainPaymentMethodDetailsValue getDetails() {
        return details;
    }

    public boolean isDefault() {
        return isDefault;
    }

    public boolean isActive() {
        return isActive;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DomainPaymentMethodEntity)) return false;
        DomainPaymentMethodEntity that = (DomainPaymentMethodEntity) o;
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "DomainPaymentMethodEntity{" +
                "id=" + id +
                ", customerId=" + customerId +
                ", type=" + type +
                ", isDefault=" + isDefault +
                ", isActive=" + isActive +
                '}';
    }
}