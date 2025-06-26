package ai.shreds.shared.value_objects;

import lombok.Value;
import lombok.Builder;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.Objects;

/**
 * Immutable value object representing an address.
 */
@Value
@Builder
public class SharedAddressValue {

    @NotBlank(message = "Street1 is required")
    @Size(max = 100, message = "Street1 must be at most 100 characters")
    String street1;

    @Size(max = 100, message = "Street2 must be at most 100 characters")
    String street2;

    @NotBlank(message = "City is required")
    @Size(max = 50, message = "City must be at most 50 characters")
    String city;

    @Size(max = 50, message = "State must be at most 50 characters")
    String state;

    @NotBlank(message = "Postal code is required")
    @Size(max = 20, message = "Postal code must be at most 20 characters")
    String postalCode;

    @NotBlank(message = "Country is required")
    @Pattern(regexp = "^[A-Z]{2}$", message = "Country must be a valid ISO 3166-1 alpha-2 code")
    String country;

    /**
     * Factory method to create a SharedAddressValue instance with validation.
     *
     * @param street1    the primary street address
     * @param city       the city name
     * @param state      the state/province (optional)
     * @param postalCode the postal/zip code
     * @param country    the ISO 3166-1 alpha-2 country code
     * @return a new SharedAddressValue instance
     * @throws IllegalArgumentException if required parameters are invalid
     */
    public static SharedAddressValue of(String street1, String city, String state, String postalCode, String country) {
        return of(street1, null, city, state, postalCode, country);
    }

    /**
     * Factory method to create a SharedAddressValue instance with validation.
     *
     * @param street1    the primary street address
     * @param street2    the secondary street address (optional)
     * @param city       the city name
     * @param state      the state/province (optional)
     * @param postalCode the postal/zip code
     * @param country    the ISO 3166-1 alpha-2 country code
     * @return a new SharedAddressValue instance
     * @throws IllegalArgumentException if required parameters are invalid
     */
    public static SharedAddressValue of(String street1, String street2, String city, String state, String postalCode, String country) {
        Objects.requireNonNull(street1, "Street1 is required");
        Objects.requireNonNull(city, "City is required");
        Objects.requireNonNull(postalCode, "Postal code is required");
        Objects.requireNonNull(country, "Country is required");
        
        if (street1.trim().isEmpty()) {
            throw new IllegalArgumentException("Street1 cannot be empty");
        }
        if (city.trim().isEmpty()) {
            throw new IllegalArgumentException("City cannot be empty");
        }
        if (postalCode.trim().isEmpty()) {
            throw new IllegalArgumentException("Postal code cannot be empty");
        }
        if (!country.matches("^[A-Z]{2}$")) {
            throw new IllegalArgumentException("Country must be a valid ISO 3166-1 alpha-2 code");
        }
        
        return new SharedAddressValue(street1, street2, city, state, postalCode, country);
    }

    /**
     * Returns the full address as a formatted string.
     *
     * @return formatted address string
     */
    public String getFullAddress() {
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
