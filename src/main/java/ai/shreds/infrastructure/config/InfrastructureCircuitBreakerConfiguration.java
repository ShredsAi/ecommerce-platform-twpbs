package ai.shreds.infrastructure.config;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class InfrastructureCircuitBreakerConfiguration {

    @Bean
    public CircuitBreakerRegistry circuitBreakerRegistry() {
        return CircuitBreakerRegistry.ofDefaults();
    }

    @Bean
    @Qualifier("stripeCircuitBreaker")
    public CircuitBreaker stripeCircuitBreaker(CircuitBreakerRegistry registry) {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
            .failureRateThreshold(50) // 50% failure rate to open circuit
            .waitDurationInOpenState(Duration.ofSeconds(30)) // wait 30 seconds in open state before transitioning to half-open
            .slidingWindowSize(10) // consider last 10 calls for failure rate
            .minimumNumberOfCalls(5) // minimum calls required before calculating failure rate
            .permittedNumberOfCallsInHalfOpenState(3) // allow 3 test calls when half-open
            .automaticTransitionFromOpenToHalfOpenEnabled(true)
            .recordExceptions(
                java.io.IOException.class, 
                java.net.ConnectException.class,
                org.springframework.web.client.RestClientException.class,
                ai.shreds.infrastructure.exceptions.InfrastructureExternalServiceException.class
            )
            .ignoreExceptions(
                // Ignore validation errors as they don't indicate service health issues
                java.lang.IllegalArgumentException.class
            )
            .build();

        return registry.circuitBreaker("stripe", config);
    }

    @Bean
    @Qualifier("paypalCircuitBreaker")
    public CircuitBreaker paypalCircuitBreaker(CircuitBreakerRegistry registry) {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
            .failureRateThreshold(50)
            .waitDurationInOpenState(Duration.ofSeconds(30))
            .slidingWindowSize(10)
            .minimumNumberOfCalls(5)
            .permittedNumberOfCallsInHalfOpenState(3)
            .automaticTransitionFromOpenToHalfOpenEnabled(true)
            .recordExceptions(
                java.io.IOException.class, 
                java.net.ConnectException.class,
                org.springframework.web.client.RestClientException.class,
                ai.shreds.infrastructure.exceptions.InfrastructureExternalServiceException.class
            )
            .ignoreExceptions(
                java.lang.IllegalArgumentException.class
            )
            .build();

        return registry.circuitBreaker("paypal", config);
    }

    @Bean
    @Qualifier("squareCircuitBreaker")
    public CircuitBreaker squareCircuitBreaker(CircuitBreakerRegistry registry) {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
            .failureRateThreshold(50)
            .waitDurationInOpenState(Duration.ofSeconds(30))
            .slidingWindowSize(10)
            .minimumNumberOfCalls(5)
            .permittedNumberOfCallsInHalfOpenState(3)
            .automaticTransitionFromOpenToHalfOpenEnabled(true)
            .recordExceptions(
                java.io.IOException.class, 
                java.net.ConnectException.class,
                org.springframework.web.client.RestClientException.class,
                ai.shreds.infrastructure.exceptions.InfrastructureExternalServiceException.class
            )
            .ignoreExceptions(
                java.lang.IllegalArgumentException.class
            )
            .build();

        return registry.circuitBreaker("square", config);
    }

    @Bean
    @Qualifier("vaultCircuitBreaker")
    public CircuitBreaker vaultCircuitBreaker(CircuitBreakerRegistry registry) {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
            .failureRateThreshold(40) // More sensitive for security service
            .waitDurationInOpenState(Duration.ofSeconds(20))
            .slidingWindowSize(10)
            .minimumNumberOfCalls(3) // React faster to token vault issues
            .permittedNumberOfCallsInHalfOpenState(2)
            .automaticTransitionFromOpenToHalfOpenEnabled(true)
            .recordExceptions(
                java.io.IOException.class, 
                java.net.ConnectException.class,
                org.springframework.web.client.RestClientException.class,
                ai.shreds.infrastructure.exceptions.InfrastructureExternalServiceException.class
            )
            .build();

        return registry.circuitBreaker("vault", config);
    }

    @Bean
    @Qualifier("threeDSCircuitBreaker")
    public CircuitBreaker threeDSCircuitBreaker(CircuitBreakerRegistry registry) {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
            .failureRateThreshold(40) // More sensitive for 3DS security
            .waitDurationInOpenState(Duration.ofSeconds(20))
            .slidingWindowSize(10)
            .minimumNumberOfCalls(3)
            .permittedNumberOfCallsInHalfOpenState(2)
            .automaticTransitionFromOpenToHalfOpenEnabled(true)
            .recordExceptions(
                java.io.IOException.class, 
                java.net.ConnectException.class,
                org.springframework.web.client.RestClientException.class,
                ai.shreds.infrastructure.exceptions.InfrastructureExternalServiceException.class
            )
            .build();

        return registry.circuitBreaker("threeds", config);
    }
}