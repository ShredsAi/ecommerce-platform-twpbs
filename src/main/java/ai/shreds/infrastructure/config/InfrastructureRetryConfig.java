package ai.shreds.infrastructure.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;

@Configuration
public class InfrastructureRetryConfig {

    @Value("${retry.max-attempts:3}")
    private int maxAttempts;

    @Value("${retry.initial-interval:500}")
    private long initialInterval;

    @Value("${retry.max-interval:5000}")
    private long maxInterval;

    @Value("${retry.multiplier:2.0}")
    private double multiplier;

    @Bean
    public RetryTemplate retryTemplate(SimpleRetryPolicy retryPolicy, ExponentialBackOffPolicy backOffPolicy) {
        RetryTemplate retryTemplate = new RetryTemplate();
        retryTemplate.setRetryPolicy(retryPolicy);
        retryTemplate.setBackOffPolicy(backOffPolicy);
        return retryTemplate;
    }

    @Bean
    public SimpleRetryPolicy retryPolicy() {
        return new SimpleRetryPolicy(maxAttempts);
    }

    @Bean
    public ExponentialBackOffPolicy backOffPolicy() {
        ExponentialBackOffPolicy policy = new ExponentialBackOffPolicy();
        policy.setInitialInterval(initialInterval);
        policy.setMaxInterval(maxInterval);
        policy.setMultiplier(multiplier);
        return policy;
    }
}