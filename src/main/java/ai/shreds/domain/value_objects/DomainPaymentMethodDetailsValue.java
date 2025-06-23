package ai.shreds.domain.value_objects;

import java.util.Objects;

/**
 * Value object representing payment method details in the domain.
 * Contains different types of payment method details based on the type.
 */
public final class DomainPaymentMethodDetailsValue {
    private final DomainPaymentMethodTypeEnum type;
    private final DomainCardDetailsValue cardDetails;
    private final DomainBankAccountDetailsValue bankAccountDetails;
    private final DomainDigitalWalletDetailsValue digitalWalletDetails;

    public DomainPaymentMethodDetailsValue(
            DomainPaymentMethodTypeEnum type,
            DomainCardDetailsValue cardDetails,
            DomainBankAccountDetailsValue bankAccountDetails,
            DomainDigitalWalletDetailsValue digitalWalletDetails) {
        this.type = Objects.requireNonNull(type, "Payment method type cannot be null");
        this.cardDetails = cardDetails;
        this.bankAccountDetails = bankAccountDetails;
        this.digitalWalletDetails = digitalWalletDetails;
        
        // Validate that only the appropriate details are set based on type
        validateDetailsForType();
    }

    public static DomainPaymentMethodDetailsValue forCard(DomainCardDetailsValue cardDetails) {
        return new DomainPaymentMethodDetailsValue(
                DomainPaymentMethodTypeEnum.CARD,
                Objects.requireNonNull(cardDetails, "Card details cannot be null"),
                null,
                null
        );
    }

    public static DomainPaymentMethodDetailsValue forBankAccount(DomainBankAccountDetailsValue bankAccountDetails) {
        return new DomainPaymentMethodDetailsValue(
                DomainPaymentMethodTypeEnum.BANK_ACCOUNT,
                null,
                Objects.requireNonNull(bankAccountDetails, "Bank account details cannot be null"),
                null
        );
    }

    public static DomainPaymentMethodDetailsValue forDigitalWallet(DomainDigitalWalletDetailsValue digitalWalletDetails) {
        return new DomainPaymentMethodDetailsValue(
                DomainPaymentMethodTypeEnum.DIGITAL_WALLET,
                null,
                null,
                Objects.requireNonNull(digitalWalletDetails, "Digital wallet details cannot be null")
        );
    }

    private void validateDetailsForType() {
        switch (type) {
            case CARD:
                if (cardDetails == null || bankAccountDetails != null || digitalWalletDetails != null) {
                    throw new IllegalArgumentException("For CARD type, only card details should be provided");
                }
                break;
            case BANK_ACCOUNT:
                if (bankAccountDetails == null || cardDetails != null || digitalWalletDetails != null) {
                    throw new IllegalArgumentException("For BANK_ACCOUNT type, only bank account details should be provided");
                }
                break;
            case DIGITAL_WALLET:
                if (digitalWalletDetails == null || cardDetails != null || bankAccountDetails != null) {
                    throw new IllegalArgumentException("For DIGITAL_WALLET type, only digital wallet details should be provided");
                }
                break;
        }
    }

    public DomainPaymentMethodTypeEnum getType() {
        return type;
    }

    public DomainCardDetailsValue getCardDetails() {
        return cardDetails;
    }

    public DomainBankAccountDetailsValue getBankAccountDetails() {
        return bankAccountDetails;
    }

    public DomainDigitalWalletDetailsValue getDigitalWalletDetails() {
        return digitalWalletDetails;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DomainPaymentMethodDetailsValue)) return false;
        DomainPaymentMethodDetailsValue that = (DomainPaymentMethodDetailsValue) o;
        return type == that.type &&
                Objects.equals(cardDetails, that.cardDetails) &&
                Objects.equals(bankAccountDetails, that.bankAccountDetails) &&
                Objects.equals(digitalWalletDetails, that.digitalWalletDetails);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, cardDetails, bankAccountDetails, digitalWalletDetails);
    }

    @Override
    public String toString() {
        return "DomainPaymentMethodDetailsValue{" +
                "type=" + type +
                ", cardDetails=" + cardDetails +
                ", bankAccountDetails=" + bankAccountDetails +
                ", digitalWalletDetails=" + digitalWalletDetails +
                '}';
    }
}