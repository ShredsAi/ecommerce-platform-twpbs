package ai.shreds.domain.value_objects;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Value object representing digital wallet details in the domain.
 */
public final class DomainDigitalWalletDetailsValue {
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "^[A-Za-z0-9+_.-]+@([A-Za-z0-9.-]+\\.[A-Za-z]{2,})$"
    );
    
    private final String walletType;
    private final String email;

    public DomainDigitalWalletDetailsValue(
            String walletType,
            String email) {
        this.walletType = Objects.requireNonNull(walletType, "walletType cannot be null");
        this.email = Objects.requireNonNull(email, "email cannot be null");
        
        validateDigitalWalletDetails();
    }

    private void validateDigitalWalletDetails() {
        if (walletType.trim().isEmpty()) {
            throw new IllegalArgumentException("walletType cannot be empty");
        }
        if (email.trim().isEmpty()) {
            throw new IllegalArgumentException("email cannot be empty");
        }
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            throw new IllegalArgumentException("email must be a valid email address");
        }
    }

    public String getWalletType() {
        return walletType;
    }

    public String getEmail() {
        return email;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DomainDigitalWalletDetailsValue)) return false;
        DomainDigitalWalletDetailsValue that = (DomainDigitalWalletDetailsValue) o;
        return walletType.equals(that.walletType) &&
                email.equals(that.email);
    }

    @Override
    public int hashCode() {
        return Objects.hash(walletType, email);
    }

    @Override
    public String toString() {
        return "DomainDigitalWalletDetailsValue{" +
                "walletType='" + walletType + '\'' +
                ", email='" + email + '\'' +
                '}';
    }
}