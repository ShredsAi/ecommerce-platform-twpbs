package ai.shreds.application.exceptions;

public class ApplicationExceptionStockNotFoundException extends RuntimeException {
    private final String skuId;
    private final String locationId;

    public ApplicationExceptionStockNotFoundException(String skuId, String locationId) {
        super(String.format("Stock record not found for SKU '%s' at location '%s'", skuId, locationId));
        this.skuId = skuId;
        this.locationId = locationId;
    }

    public String getSkuId() {
        return skuId;
    }

    public String getLocationId() {
        return locationId;
    }
}