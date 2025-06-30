package ai.shreds.infrastructure.external_services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;

import ai.shreds.application.ports.ApplicationCacheOutputPort;
import ai.shreds.shared.dtos.SharedStockLevelDTO;
import ai.shreds.shared.value_objects.SharedCacheKey;
import ai.shreds.infrastructure.exceptions.InfrastructureExceptionExternalServiceError;

import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Service
public class InfrastructureRedisCache implements ApplicationCacheOutputPort {

    private static final Logger log = LoggerFactory.getLogger(InfrastructureRedisCache.class);

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;
    private final long cacheTTL;

    public InfrastructureRedisCache(
            RedisTemplate<String, String> redisTemplate,
            ObjectMapper objectMapper,
            @Value("${cache.ttl:60}") long cacheTTL) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.cacheTTL = cacheTTL;
    }

    @Override
    public Optional<SharedStockLevelDTO> get(SharedCacheKey key) {
        try {
            ValueOperations<String, String> valueOps = redisTemplate.opsForValue();
            String json = valueOps.get(key.toKey());
            if (json != null) {
                SharedStockLevelDTO dto = objectMapper.readValue(json, SharedStockLevelDTO.class);
                return Optional.of(dto);
            }
        } catch (DataAccessException | JsonProcessingException e) {
            log.error("Failed to get cache for key: {}", key, e);
            throw new InfrastructureExceptionExternalServiceError("Redis", "Failed to get from cache: " + e.getMessage(), e);
        }
        return Optional.empty();
    }

    @Override
    public void put(SharedCacheKey key, SharedStockLevelDTO value) {
        try {
            ValueOperations<String, String> valueOps = redisTemplate.opsForValue();
            String json = objectMapper.writeValueAsString(value);
            valueOps.set(key.toKey(), json, cacheTTL, TimeUnit.SECONDS);
            log.debug("Cached value for key: {} with TTL: {} seconds", key.toKey(), cacheTTL);
        } catch (DataAccessException | JsonProcessingException e) {
            log.error("Failed to put cache for key: {}", key, e);
            throw new InfrastructureExceptionExternalServiceError("Redis", "Failed to put into cache: " + e.getMessage(), e);
        }
    }

    @Override
    public void evict(SharedCacheKey key) {
        try {
            redisTemplate.delete(key.toKey());
            log.debug("Evicted cache for key: {}", key.toKey());
        } catch (DataAccessException e) {
            log.error("Failed to evict cache for key: {}", key, e);
            throw new InfrastructureExceptionExternalServiceError("Redis", "Failed to evict cache: " + e.getMessage(), e);
        }
    }

    @Override
    public void evictAll(String pattern) {
        try {
            Set<String> keys = redisTemplate.keys(pattern);
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
                log.debug("Evicted all cache matching pattern: {}", pattern);
            }
        } catch (DataAccessException e) {
            log.error("Failed to evict all caches matching pattern: {}", pattern, e);
            throw new InfrastructureExceptionExternalServiceError("Redis", "Failed to evict all caches: " + e.getMessage(), e);
        }
    }
}