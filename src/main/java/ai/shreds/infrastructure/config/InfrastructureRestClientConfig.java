package ai.shreds.infrastructure.config;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;

@Configuration
public class InfrastructureRestClientConfig {

    @Bean
    public RestTemplate restTemplate() {
        return configureTimeouts(new RestTemplateBuilder()).build();
    }

    @Bean
    public WebClient webClient() {
        return WebClient.builder()
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(1024 * 1024))
                .build();
    }

    @Bean
    public CircuitBreakerRegistry circuitBreakerRegistry() {
        return CircuitBreakerRegistry.of(configureResilience4j());
    }

    @Bean
    public RetryRegistry retryRegistry() {
        RetryConfig retryConfig = RetryConfig.custom()
                .maxAttempts(3)
                .waitDuration(Duration.ofMillis(500))
                .retryOnResult(response -> false)
                .retryExceptions(Exception.class)
                .build();
        
        RetryRegistry retryRegistry = RetryRegistry.of(retryConfig);
        
        // Configure specific retry instances
        retryRegistry.retry("payment-service", RetryConfig.custom()
                .maxAttempts(3)
                .waitDuration(Duration.ofMillis(1000))
                .build());
                
        retryRegistry.retry("notification-service", RetryConfig.custom()
                .maxAttempts(2)
                .waitDuration(Duration.ofMillis(500))
                .build());
        
        return retryRegistry;
    }

    @Bean
    public CircuitBreaker paymentServiceCircuitBreaker(CircuitBreakerRegistry registry) {
        return registry.circuitBreaker("payment-service");
    }

    @Bean
    public CircuitBreaker inventoryServiceCircuitBreaker(CircuitBreakerRegistry registry) {
        return registry.circuitBreaker("inventory-service");
    }

    @Bean
    public CircuitBreaker orderServiceCircuitBreaker(CircuitBreakerRegistry registry) {
        return registry.circuitBreaker("order-service");
    }

    @Bean
    public Retry paymentServiceRetry(RetryRegistry registry) {
        return registry.retry("payment-service");
    }

    @Bean
    public Retry notificationServiceRetry(RetryRegistry registry) {
        return registry.retry("notification-service");
    }

    private RestTemplateBuilder configureTimeouts(RestTemplateBuilder builder) {
        return builder
                .setConnectTimeout(Duration.ofSeconds(5))
                .setReadTimeout(Duration.ofSeconds(10))
                .additionalInterceptors((request, body, execution) -> {
                    request.getHeaders().add("User-Agent", "order-cancellation-returns-service/1.0");
                    return execution.execute(request, body);
                });
    }

    private CircuitBreakerConfig configureResilience4j() {
        return CircuitBreakerConfig.custom()
                .failureRateThreshold(50)
                .waitDurationInOpenState(Duration.ofSeconds(30))
                .slidingWindowSize(100)
                .minimumNumberOfCalls(10)
                .permittedNumberOfCallsInHalfOpenState(5)
                .automaticTransitionFromOpenToHalfOpenEnabled(true)
                .slowCallRateThreshold(50)
                .slowCallDurationThreshold(Duration.ofSeconds(2))
                .recordExceptions(Exception.class)
                .build();
    }
}