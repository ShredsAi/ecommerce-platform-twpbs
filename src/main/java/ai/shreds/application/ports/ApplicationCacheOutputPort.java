package ai.shreds.application.ports;

import ai.shreds.shared.value_objects.SharedCacheKey;
import ai.shreds.shared.dtos.SharedStockLevelDTO;
import java.util.Optional;

public interface ApplicationCacheOutputPort {
    /**
     * Retrieves a cached stock level DTO by key.
     *
     * @param key the cache key
     * @return optional DTO if present
     */
    Optional<SharedStockLevelDTO> get(SharedCacheKey key);

    /**
     * Puts a stock level DTO into cache under the given key.
     *
     * @param key the cache key
     * @param value the DTO to cache
     */
    void put(SharedCacheKey key, SharedStockLevelDTO value);

    /**
     * Evicts a single cache entry by key.
     *
     * @param key the cache key to evict
     */
    void evict(SharedCacheKey key);

    /**
     * Evicts all cache entries matching the pattern.
     *
     * @param pattern the key pattern to evict
     */
    void evictAll(String pattern);
}