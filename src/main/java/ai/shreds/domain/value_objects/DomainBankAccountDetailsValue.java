package ai.shreds.domain.value_objects;

import java.util.Objects;

/**
 * Value object representing bank account details in the domain.
 */
public final class DomainBankAccountDetailsValue {
    private final String last4;
    private final String bankName;
    private final String accountType;

    public DomainBankAccountDetailsValue(
            String last4,
            String bankName,
            String accountType) {
        this.last4 = Objects.requireNonNull(last4, "last4 cannot be null");
        this.bankName = Objects.requireNonNull(bankName, "bankName cannot be null");
        this.accountType = Objects.requireNonNull(accountType, "accountType cannot be null");
        
        validateBankAccountDetails();
    }

    private void validateBankAccountDetails() {
        if (last4.length() != 4 || !last4.matches("\\d{4}")) {
            throw new IllegalArgumentException("last4 must be exactly 4 digits");
        }
        if (bankName.trim().isEmpty()) {
            throw new IllegalArgumentException("bankName cannot be empty");
        }
        if (accountType.trim().isEmpty()) {
            throw new IllegalArgumentException("accountType cannot be empty");
        }
    }

    public String getLast4() {
        return last4;
    }

    public String getBankName() {
        return bankName;
    }

    public String getAccountType() {
        return accountType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DomainBankAccountDetailsValue)) return false;
        DomainBankAccountDetailsValue that = (DomainBankAccountDetailsValue) o;
        return last4.equals(that.last4) &&
                bankName.equals(that.bankName) &&
                accountType.equals(that.accountType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(last4, bankName, accountType);
    }

    @Override
    public String toString() {
        return "DomainBankAccountDetailsValue{" +
                "last4='****" + last4 + '\'' +
                ", bankName='" + bankName + '\'' +
                ", accountType='" + accountType + '\'' +
                '}';
    }
}