package ai.shreds.domain.entities;

import ai.shreds.domain.exceptions.DomainExceptionInvalidState;
import ai.shreds.domain.value_objects.DomainValueAddress;
import ai.shreds.domain.value_objects.DomainValueLocationId;
import ai.shreds.domain.value_objects.DomainValueLocationName;
import ai.shreds.shared.enums.SharedEnumLocationType;
import java.time.Instant;

public class DomainEntityLocation {
    private final DomainValueLocationId locationId;
    private DomainValueLocationName name;
    private SharedEnumLocationType type;
    private DomainValueAddress address;
    private boolean isActive;
    private final Instant createdAt;

    private DomainEntityLocation(DomainValueLocationId locationId,
                                 DomainValueLocationName name,
                                 SharedEnumLocationType type,
                                 DomainValueAddress address,
                                 boolean isActive,
                                 Instant createdAt) {
        this.locationId = locationId;
        this.name = name;
        this.type = type;
        this.address = address;
        this.isActive = isActive;
        this.createdAt = createdAt;
    }

    public static DomainEntityLocation create(DomainValueLocationId locationId,
                                              DomainValueLocationName name,
                                              SharedEnumLocationType type,
                                              DomainValueAddress address) {
        return new DomainEntityLocation(locationId, name, type, address, true, Instant.now());
    }

    public static DomainEntityLocation reconstruct(DomainValueLocationId locationId,
                                                   DomainValueLocationName name,
                                                   SharedEnumLocationType type,
                                                   DomainValueAddress address,
                                                   boolean isActive,
                                                   Instant createdAt) {
        return new DomainEntityLocation(locationId, name, type, address, isActive, createdAt);
    }

    public void activate() {
        if (!isActive) {
            isActive = true;
        }
    }

    public void deactivate() {
        if (isActive) {
            isActive = false;
        }
    }

    public void updateAddress(DomainValueAddress newAddress) {
        if (!isActive) {
            throw new DomainExceptionInvalidState("INACTIVE", "updateAddress");
        }
        this.address = newAddress;
    }

    public void updateName(DomainValueLocationName newName) {
        if (!isActive) {
            throw new DomainExceptionInvalidState("INACTIVE", "updateName");
        }
        this.name = newName;
    }

    public void updateType(SharedEnumLocationType newType) {
        if (!isActive) {
            throw new DomainExceptionInvalidState("INACTIVE", "updateType");
        }
        this.type = newType;
    }

    public DomainValueLocationId getLocationId() {
        return locationId;
    }

    public DomainValueLocationName getName() {
        return name;
    }

    public SharedEnumLocationType getType() {
        return type;
    }

    public DomainValueAddress getAddress() {
        return address;
    }

    public boolean isActive() {
        return isActive;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}