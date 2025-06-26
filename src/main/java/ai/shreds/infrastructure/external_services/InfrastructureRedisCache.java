package ai.shreds.infrastructure.external_services;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Service
public class InfrastructureRedisCache {

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    public InfrastructureRedisCache(RedisTemplate<String, String> redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    public <T> void put(String key, T value, Duration ttl) {
        try {
            String jsonValue = objectMapper.writeValueAsString(value);
            redisTemplate.opsForValue().set(key, jsonValue, ttl.toSeconds(), TimeUnit.SECONDS);
        } catch (Exception e) {
            throw new InfrastructureCacheException("Failed to cache object with key: " + key, e);
        }
    }

    public <T> Optional<T> get(String key, Class<T> type) {
        try {
            String jsonValue = redisTemplate.opsForValue().get(key);
            if (jsonValue != null) {
                T value = objectMapper.readValue(jsonValue, type);
                return Optional.of(value);
            }
            return Optional.empty();
        } catch (Exception e) {
            throw new InfrastructureCacheException("Failed to retrieve object from cache with key: " + key, e);
        }
    }

    public void evict(String key) {
        try {
            redisTemplate.delete(key);
        } catch (Exception e) {
            throw new InfrastructureCacheException("Failed to evict key: " + key, e);
        }
    }

    public boolean exists(String key) {
        try {
            return Boolean.TRUE.equals(redisTemplate.hasKey(key));
        } catch (Exception e) {
            throw new InfrastructureCacheException("Failed to check existence of key: " + key, e);
        }
    }
}