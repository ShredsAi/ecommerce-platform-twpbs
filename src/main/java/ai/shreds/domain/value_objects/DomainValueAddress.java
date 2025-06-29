package ai.shreds.domain.value_objects;

import ai.shreds.domain.exceptions.DomainExceptionInvariantViolation;
import java.util.Objects;

public final class DomainValueAddress {
    private final String street;
    private final String city;
    private final String state;
    private final String postalCode;
    private final String country;

    public DomainValueAddress(String street, String city, String state, String postalCode, String country) {
        validate(street, city, state, postalCode, country);
        this.street = street;
        this.city = city;
        this.state = state;
        this.postalCode = postalCode;
        this.country = country;
    }

    private void validate(String street, String city, String state, String postalCode, String country) {
        if (street == null || street.trim().isEmpty()) {
            throw new DomainExceptionInvariantViolation("Street cannot be null or empty");
        }
        if (city == null || city.trim().isEmpty()) {
            throw new DomainExceptionInvariantViolation("City cannot be null or empty");
        }
        if (state == null || state.trim().isEmpty()) {
            throw new DomainExceptionInvariantViolation("State cannot be null or empty");
        }
        if (postalCode == null || postalCode.trim().isEmpty()) {
            throw new DomainExceptionInvariantViolation("Postal code cannot be null or empty");
        }
        if (country == null || country.trim().isEmpty()) {
            throw new DomainExceptionInvariantViolation("Country cannot be null or empty");
        }
    }

    public String getStreet() { return street; }
    public String getCity() { return city; }
    public String getState() { return state; }
    public String getPostalCode() { return postalCode; }
    public String getCountry() { return country; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DomainValueAddress)) return false;
        DomainValueAddress that = (DomainValueAddress) o;
        return street.equals(that.street) && city.equals(that.city)
            && state.equals(that.state) && postalCode.equals(that.postalCode)
            && country.equals(that.country);
    }

    @Override
    public int hashCode() {
        return Objects.hash(street, city, state, postalCode, country);
    }

    @Override
    public String toString() {
        return street + ", " + city + ", " + state + " " + postalCode + ", " + country;
    }
}