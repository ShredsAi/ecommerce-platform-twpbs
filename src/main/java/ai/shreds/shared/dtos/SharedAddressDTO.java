package ai.shreds.shared.dtos;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import ai.shreds.domain.value_objects.DomainAddressValue;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SharedAddressDTO {
    private String street1;
    private String street2;
    private String city;
    private String state;
    private String postalCode;
    private String country;

    public DomainAddressValue toDomainValue() {
        DomainAddressValue address = new DomainAddressValue(street1, street2, city, state, postalCode, country);
        address.validate();
        return address;
    }

    public static SharedAddressDTO fromDomainValue(DomainAddressValue address) {
        return new SharedAddressDTO(
            address.getStreet1(),
            address.getStreet2(),
            address.getCity(),
            address.getState(),
            address.getPostalCode(),
            address.getCountry()
        );
    }
}
