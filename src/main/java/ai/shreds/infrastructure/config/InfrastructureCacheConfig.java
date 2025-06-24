package ai.shreds.infrastructure.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
@EnableCaching
public class InfrastructureCacheConfig {

    @Bean
    public Caffeine<Object, Object> caffeineConfig() {
        return Caffeine.newBuilder()
                .initialCapacity(100)
                .maximumSize(1000)
                .expireAfterAccess(10, TimeUnit.MINUTES);
    }

    @Bean
    public CaffeineCacheManager caffeineCacheManager(Caffeine<Object, Object> caffeine) {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager("orderCache");
        cacheManager.setCaffeine(caffeine);
        return cacheManager;
    }

    @Bean
    public CacheManager cacheManager(CaffeineCacheManager caffeineCacheManager) {
        return caffeineCacheManager;
    }

    @Bean
    public Cache orderCacheConfig(CacheManager cacheManager) {
        return cacheManager.getCache("orderCache");
    }
}