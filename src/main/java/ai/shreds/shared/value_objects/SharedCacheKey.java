package ai.shreds.shared.value_objects;

public final class SharedCacheKey {
    private final String prefix;
    private final String skuId;
    private final String locationId;

    public SharedCacheKey(String prefix, String skuId, String locationId) {
        this.prefix = prefix;
        this.skuId = skuId;
        this.locationId = locationId;
    }

    public String toKey() {
        return String.format("%s:%s:%s", prefix, skuId, locationId);
    }

    public String pattern() {
        return prefix + ":*";
    }

    public String getPrefix() {
        return prefix;
    }

    public String getSkuId() {
        return skuId;
    }

    public String getLocationId() {
        return locationId;
    }
}