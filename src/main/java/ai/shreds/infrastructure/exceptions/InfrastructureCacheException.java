package ai.shreds.infrastructure.exceptions;

public class InfrastructureCacheException extends RuntimeException {

    private final String cacheKey;
    private final String operation;

    public InfrastructureCacheException(String message, Throwable cause) {
        super(message, cause);
        this.cacheKey = extractCacheKey(message);
        this.operation = extractOperation(message);
    }

    public InfrastructureCacheException(String message) {
        super(message);
        this.cacheKey = extractCacheKey(message);
        this.operation = extractOperation(message);
    }

    private String extractCacheKey(String message) {
        if (message != null && message.contains("key:")) {
            return message.substring(message.indexOf("key:") + 5).trim();
        }
        return "unknown";
    }

    private String extractOperation(String message) {
        if (message == null) {
            return "unknown";
        }
        if (message.contains("cache")) return "CACHE";
        if (message.contains("retrieve")) return "GET";
        if (message.contains("evict")) return "DELETE";
        if (message.contains("check existence")) return "EXISTS";
        return "unknown";
    }

    public String getCacheKey() {
        return cacheKey;
    }

    public String getOperation() {
        return operation;
    }
}