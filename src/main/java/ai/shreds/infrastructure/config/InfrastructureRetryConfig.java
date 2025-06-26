package ai.shreds.infrastructure.config;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class InfrastructureRetryConfig {

    @Bean
    public RetryRegistry retryRegistry() {
        return RetryRegistry.ofDefaults();
    }

    @Bean
    public CircuitBreakerRegistry circuitBreakerRegistry() {
        return CircuitBreakerRegistry.ofDefaults();
    }

    @Bean
    public Retry paymentServiceRetry(RetryRegistry retryRegistry) {
        RetryConfig config = RetryConfig.custom()
                .maxAttempts(3)
                .waitDuration(Duration.ofSeconds(2))
                .exponentialBackoffMultiplier(2.0)
                .retryOnException(throwable -> 
                    throwable instanceof RuntimeException || 
                    throwable instanceof java.net.ConnectException ||
                    throwable instanceof java.util.concurrent.TimeoutException
                )
                .build();
        
        return retryRegistry.retry("payment-service", config);
    }

    @Bean
    public CircuitBreaker paymentServiceCircuitBreaker(CircuitBreakerRegistry circuitBreakerRegistry) {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .failureRateThreshold(50)
                .waitDurationInOpenState(Duration.ofMillis(30000))
                .slidingWindowSize(10)
                .minimumNumberOfCalls(5)
                .permittedNumberOfCallsInHalfOpenState(3)
                .build();
        
        return circuitBreakerRegistry.circuitBreaker("payment-service", config);
    }

    @Bean
    public Retry inventoryServiceRetry(RetryRegistry retryRegistry) {
        RetryConfig config = RetryConfig.custom()
                .maxAttempts(3)
                .waitDuration(Duration.ofSeconds(1))
                .exponentialBackoffMultiplier(1.5)
                .retryOnException(throwable -> 
                    throwable instanceof RuntimeException || 
                    throwable instanceof java.net.ConnectException ||
                    throwable instanceof java.util.concurrent.TimeoutException
                )
                .build();
        
        return retryRegistry.retry("inventory-service", config);
    }

    @Bean
    public CircuitBreaker inventoryServiceCircuitBreaker(CircuitBreakerRegistry circuitBreakerRegistry) {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .failureRateThreshold(60)
                .waitDurationInOpenState(Duration.ofMillis(20000))
                .slidingWindowSize(8)
                .minimumNumberOfCalls(4)
                .permittedNumberOfCallsInHalfOpenState(2)
                .build();
        
        return circuitBreakerRegistry.circuitBreaker("inventory-service", config);
    }

    @Bean
    public Retry shippingServiceRetry(RetryRegistry retryRegistry) {
        RetryConfig config = RetryConfig.custom()
                .maxAttempts(2)
                .waitDuration(Duration.ofSeconds(3))
                .exponentialBackoffMultiplier(2.0)
                .retryOnException(throwable -> 
                    throwable instanceof RuntimeException || 
                    throwable instanceof io.grpc.StatusRuntimeException
                )
                .build();
        
        return retryRegistry.retry("shipping-service", config);
    }
}