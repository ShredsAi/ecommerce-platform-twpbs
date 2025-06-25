package ai.shreds.domain.value_objects;

import ai.shreds.domain.entities.DomainAddressEntity;
import ai.shreds.domain.exceptions.DomainValidationException;
import lombok.Getter;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Getter
public class DomainAddressValue {
    private final String street1;
    private final String street2;
    private final String city;
    private final String state;
    private final String postalCode;
    private final String country;

    public DomainAddressValue(String street1, String street2, String city, String state, String postalCode, String country) {
        validate(street1, street2, city, state, postalCode, country);
        this.street1 = street1;
        this.street2 = street2;
        this.city = city;
        this.state = state;
        this.postalCode = postalCode;
        this.country = country != null ? country.toUpperCase() : null;
    }

    public void validate() {
        validate(this.street1, this.street2, this.city, this.state, this.postalCode, this.country);
    }

    private void validate(String street1, String street2, String city, String state, String postalCode, String country) {
        if (street1 == null || street1.trim().isEmpty()) {
            throw new DomainValidationException("Street1 is required", 
                List.of("street1 cannot be null or empty"));
        }
        if (street1.length() > 255) {
            throw new DomainValidationException("Street1 cannot exceed 255 characters", 
                List.of("street1 length must be <= 255"));
        }
        if (city == null || city.trim().isEmpty()) {
            throw new DomainValidationException("City is required", 
                List.of("city cannot be null or empty"));
        }
        if (city.length() > 100) {
            throw new DomainValidationException("City cannot exceed 100 characters", 
                List.of("city length must be <= 100"));
        }
        if (postalCode == null || postalCode.trim().isEmpty()) {
            throw new DomainValidationException("Postal code is required", 
                List.of("postalCode cannot be null or empty"));
        }
        if (postalCode.length() > 20) {
            throw new DomainValidationException("Postal code cannot exceed 20 characters", 
                List.of("postalCode length must be <= 20"));
        }
        if (country == null || country.trim().isEmpty() || country.length() != 2) {
            throw new DomainValidationException("Country must be a valid 2-character ISO code", 
                List.of("country must be a 2-character ISO-3166 code"));
        }
        if (!country.matches("^[A-Z]{2}$")) {
            throw new DomainValidationException("Country must contain only uppercase letters", 
                List.of("country must be in uppercase format"));
        }
        if (street2 != null && street2.length() > 255) {
            throw new DomainValidationException("Street2 cannot exceed 255 characters", 
                List.of("street2 length must be <= 255"));
        }
        if (state != null && state.length() > 100) {
            throw new DomainValidationException("State cannot exceed 100 characters", 
                List.of("state length must be <= 100"));
        }
    }

    public DomainAddressEntity toEntity() {
        return DomainAddressEntity.builder()
            .addressId(UUID.randomUUID()) // Generate UUID immediately
            .street1(this.street1)
            .street2(this.street2)
            .city(this.city)
            .state(this.state)
            .postalCode(this.postalCode)
            .country(this.country)
            .build();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DomainAddressValue that = (DomainAddressValue) o;
        return Objects.equals(street1, that.street1) &&
               Objects.equals(street2, that.street2) &&
               Objects.equals(city, that.city) &&
               Objects.equals(state, that.state) &&
               Objects.equals(postalCode, that.postalCode) &&
               Objects.equals(country, that.country);
    }

    @Override
    public int hashCode() {
        return Objects.hash(street1, street2, city, state, postalCode, country);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(street1);
        if (street2 != null && !street2.trim().isEmpty()) {
            sb.append(", ").append(street2);
        }
        sb.append(", ").append(city);
        if (state != null && !state.trim().isEmpty()) {
            sb.append(", ").append(state);
        }
        sb.append(" ").append(postalCode);
        sb.append(", ").append(country);
        return sb.toString();
    }
}