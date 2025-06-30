package ai.shreds.domain.value_objects;

import ai.shreds.domain.exceptions.DomainExceptionInvariantViolation;
import java.util.Objects;

public final class DomainValueReconciliationError {
    private final String skuId;
    private final String locationId;
    private final String errorMessage;
    private final String errorCode;

    public DomainValueReconciliationError(String skuId, String locationId, String errorMessage, String errorCode) {
        if (skuId == null || skuId.trim().isEmpty()) {
            throw new DomainExceptionInvariantViolation("SKU ID cannot be null or empty");
        }
        if (locationId == null || locationId.trim().isEmpty()) {
            throw new DomainExceptionInvariantViolation("Location ID cannot be null or empty");
        }
        if (errorMessage == null || errorMessage.trim().isEmpty()) {
            throw new DomainExceptionInvariantViolation("Error message cannot be null or empty");
        }
        this.skuId = skuId;
        this.locationId = locationId;
        this.errorMessage = errorMessage;
        this.errorCode = errorCode;
    }

    public String getSkuId() {
        return skuId;
    }

    public String getLocationId() {
        return locationId;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public String getErrorCode() {
        return errorCode;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DomainValueReconciliationError)) return false;
        DomainValueReconciliationError that = (DomainValueReconciliationError) o;
        return skuId.equals(that.skuId) && locationId.equals(that.locationId)
            && errorMessage.equals(that.errorMessage) && Objects.equals(errorCode, that.errorCode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(skuId, locationId, errorMessage, errorCode);
    }

    @Override
    public String toString() {
        return String.format("ReconciliationError[skuId=%s, locationId=%s, message=%s, code=%s]", 
            skuId, locationId, errorMessage, errorCode);
    }
}