package ai.shreds.infrastructure.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.concurrent.TimeUnit;
import java.util.Arrays;

@Configuration
@EnableCaching
public class InfrastructureCacheConfig {

    @Value("${cache.stock.ttl-seconds:300}")
    private long stockCacheTimeToLive;
    
    @Value("${cache.stock.max-size:50000}")
    private long stockCacheMaxSize;
    
    @Value("${cache.rules.ttl-seconds:3600}")
    private long rulesCacheTimeToLive;
    
    @Value("${cache.rules.max-size:10000}")
    private long rulesCacheMaxSize;

    @Bean
    @Primary
    public Caffeine<Object, Object> defaultCaffeineConfig() {
        return Caffeine.newBuilder()
                .expireAfterWrite(stockCacheTimeToLive, TimeUnit.SECONDS)
                .maximumSize(stockCacheMaxSize)
                .recordStats();
    }
    
    @Bean
    public Caffeine<Object, Object> stockCaffeineConfig() {
        return Caffeine.newBuilder()
                .expireAfterWrite(stockCacheTimeToLive, TimeUnit.SECONDS)
                .maximumSize(stockCacheMaxSize)
                .recordStats();
    }
    
    @Bean
    public Caffeine<Object, Object> rulesCaffeineConfig() {
        return Caffeine.newBuilder()
                .expireAfterWrite(rulesCacheTimeToLive, TimeUnit.SECONDS)
                .maximumSize(rulesCacheMaxSize)
                .recordStats();
    }

    @Bean
    @Primary
    public CacheManager cacheManager() {
        CaffeineCacheManager manager = new CaffeineCacheManager();
        manager.setCaffeine(defaultCaffeineConfig());
        manager.setCacheNames(Arrays.asList(
            "stock-levels",
            "safety-rules", 
            "sku-validation",
            "location-validation"
        ));
        return manager;
    }
}
