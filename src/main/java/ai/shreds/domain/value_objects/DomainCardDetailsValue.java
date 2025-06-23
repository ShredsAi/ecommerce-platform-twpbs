package ai.shreds.domain.value_objects;

import java.util.Objects;

/**
 * Value object representing card details in the domain.
 */
public final class DomainCardDetailsValue {
    private final String last4;
    private final String brand;
    private final Integer expiryMonth;
    private final Integer expiryYear;

    public DomainCardDetailsValue(
            String last4,
            String brand,
            Integer expiryMonth,
            Integer expiryYear) {
        this.last4 = Objects.requireNonNull(last4, "last4 cannot be null");
        this.brand = Objects.requireNonNull(brand, "brand cannot be null");
        this.expiryMonth = Objects.requireNonNull(expiryMonth, "expiryMonth cannot be null");
        this.expiryYear = Objects.requireNonNull(expiryYear, "expiryYear cannot be null");
        
        validateCardDetails();
    }

    private void validateCardDetails() {
        if (last4.length() != 4 || !last4.matches("\\d{4}")) {
            throw new IllegalArgumentException("last4 must be exactly 4 digits");
        }
        if (brand.trim().isEmpty()) {
            throw new IllegalArgumentException("brand cannot be empty");
        }
        if (expiryMonth < 1 || expiryMonth > 12) {
            throw new IllegalArgumentException("expiryMonth must be between 1 and 12");
        }
        if (expiryYear < 2020 || expiryYear > 2100) {
            throw new IllegalArgumentException("expiryYear must be a valid future year");
        }
    }

    public String getLast4() {
        return last4;
    }

    public String getBrand() {
        return brand;
    }

    public Integer getExpiryMonth() {
        return expiryMonth;
    }

    public Integer getExpiryYear() {
        return expiryYear;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DomainCardDetailsValue)) return false;
        DomainCardDetailsValue that = (DomainCardDetailsValue) o;
        return last4.equals(that.last4) &&
                brand.equals(that.brand) &&
                expiryMonth.equals(that.expiryMonth) &&
                expiryYear.equals(that.expiryYear);
    }

    @Override
    public int hashCode() {
        return Objects.hash(last4, brand, expiryMonth, expiryYear);
    }

    @Override
    public String toString() {
        return "DomainCardDetailsValue{" +
                "last4='****" + last4 + '\'' +
                ", brand='" + brand + '\'' +
                ", expiryMonth=" + expiryMonth +
                ", expiryYear=" + expiryYear +
                '}';
    }
}