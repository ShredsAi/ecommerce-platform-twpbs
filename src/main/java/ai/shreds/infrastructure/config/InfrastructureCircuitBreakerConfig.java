package ai.shreds.infrastructure.config;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class InfrastructureCircuitBreakerConfig {

    @Value("${circuit-breaker.failure-rate-threshold:50.0}")
    private float failureRateThreshold;

    @Value("${circuit-breaker.wait-duration-in-open-state:30000}")
    private long waitDurationInOpenState;

    @Value("${circuit-breaker.sliding-window-size:10}")
    private int slidingWindowSize;

    @Value("${circuit-breaker.minimum-number-of-calls:5}")
    private int minimumNumberOfCalls;

    @Bean
    public CircuitBreakerConfig circuitBreakerConfig() {
        return CircuitBreakerConfig.custom()
                .failureRateThreshold(failureRateThreshold)
                .waitDurationInOpenState(Duration.ofMillis(waitDurationInOpenState))
                .slidingWindowSize(slidingWindowSize)
                .minimumNumberOfCalls(minimumNumberOfCalls)
                .build();
    }

    @Bean
    public CircuitBreakerRegistry circuitBreakerRegistry(CircuitBreakerConfig circuitBreakerConfig) {
        return CircuitBreakerRegistry.of(circuitBreakerConfig);
    }

    @Bean("inventoryCircuitBreaker")
    public CircuitBreaker inventoryCircuitBreaker(CircuitBreakerRegistry registry) {
        return registry.circuitBreaker("inventoryService");
    }

    @Bean("pricingCircuitBreaker")
    public CircuitBreaker pricingCircuitBreaker(CircuitBreakerRegistry registry) {
        return registry.circuitBreaker("pricingService");
    }

    @Bean
    public CircuitBreaker circuitBreaker(CircuitBreakerRegistry registry) {
        return registry.circuitBreaker("defaultCircuitBreaker");
    }
}