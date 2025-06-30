package ai.shreds.infrastructure.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.ToString;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class InfrastructureJpaEmbeddableAddress {

    @NotNull
    @Size(min = 1, max = 255)
    @Column(name = "street", length = 255)
    private String street;

    @NotNull
    @Size(min = 1, max = 100)
    @Column(name = "city", length = 100)
    private String city;

    @Size(max = 100)
    @Column(name = "state", length = 100)
    private String state;

    @Size(max = 20)
    @Column(name = "postal_code", length = 20)
    private String postalCode;

    @NotNull
    @Size(min = 1, max = 100)
    @Column(name = "country", length = 100)
    private String country;
    
    // Validation method
    public void validateAddress() {
        if (street == null || street.trim().isEmpty()) {
            throw new IllegalArgumentException("Street cannot be null or empty");
        }
        if (city == null || city.trim().isEmpty()) {
            throw new IllegalArgumentException("City cannot be null or empty");
        }
        if (country == null || country.trim().isEmpty()) {
            throw new IllegalArgumentException("Country cannot be null or empty");
        }
    }
    
    // Helper method to format address as string
    public String getFormattedAddress() {
        StringBuilder sb = new StringBuilder();
        sb.append(street);
        if (city != null) {
            sb.append(", ").append(city);
        }
        if (state != null && !state.trim().isEmpty()) {
            sb.append(", ").append(state);
        }
        if (postalCode != null && !postalCode.trim().isEmpty()) {
            sb.append(" ").append(postalCode);
        }
        if (country != null) {
            sb.append(", ").append(country);
        }
        return sb.toString();
    }
}
