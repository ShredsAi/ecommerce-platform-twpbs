package ai.shreds.domain.entities;

import ai.shreds.domain.value_objects.DomainAddressValue;
import ai.shreds.domain.exceptions.DomainValidationException;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "addresses")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DomainAddressEntity {

    @Id
    @Column(name = "address_id")
    private UUID addressId;

    @Column(name = "street1", nullable = false, length = 255)
    private String street1;

    @Column(name = "street2", length = 255)
    private String street2;

    @Column(name = "city", nullable = false, length = 100)
    private String city;

    @Column(name = "state", length = 100)
    private String state;

    @Column(name = "postal_code", nullable = false, length = 20)
    private String postalCode;

    @Column(name = "country", nullable = false, length = 2)
    private String country;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (addressId == null) {
            addressId = UUID.randomUUID();
        }
        createdAt = LocalDateTime.now();
        validateBusinessRules();
    }

    @PreUpdate
    protected void onUpdate() {
        validateBusinessRules();
    }

    private void validateBusinessRules() {
        if (street1 == null || street1.trim().isEmpty()) {
            throw new DomainValidationException("Street1 is required", 
                List.of("street1 cannot be null or empty"));
        }
        if (city == null || city.trim().isEmpty()) {
            throw new DomainValidationException("City is required", 
                List.of("city cannot be null or empty"));
        }
        if (postalCode == null || postalCode.trim().isEmpty()) {
            throw new DomainValidationException("Postal code is required", 
                List.of("postalCode cannot be null or empty"));
        }
        if (country == null || country.trim().isEmpty() || country.length() != 2) {
            throw new DomainValidationException("Country must be a valid 2-character ISO code", 
                List.of("country must be a 2-character ISO-3166 code"));
        }
    }

    public DomainAddressValue toValue() {
        return new DomainAddressValue(street1, street2, city, state, postalCode, country);
    }

    public static DomainAddressEntity fromValue(DomainAddressValue addressValue) {
        return DomainAddressEntity.builder()
            .street1(addressValue.getStreet1())
            .street2(addressValue.getStreet2())
            .city(addressValue.getCity())
            .state(addressValue.getState())
            .postalCode(addressValue.getPostalCode())
            .country(addressValue.getCountry())
            .build();
    }
}