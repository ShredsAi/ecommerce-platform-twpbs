package ai.shreds;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.context.annotation.Bean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

/**
 * Main Spring Boot application class for the Order Creation Shred.
 *
 * This service is responsible for:
 * - Consuming CartCheckedOut events from Kafka
 * - Validating inventory and calculating pricing via gRPC calls
 * - Creating and persisting Order aggregates
 * - Publishing OrderCreated/OrderCreationFailed events
 *
 * The application follows hexagonal architecture with clear separation between
 * adapters, application services, and domain logic.
 */
@SpringBootApplication(
    scanBasePackages = {
        "ai.shreds.adapters",
        "ai.shreds.application",
        "ai.shreds.domain",
        "ai.shreds.infrastructure",
        "ai.shreds.shared"
    }
)
@EnableJpaRepositories(
    basePackages = "ai.shreds.infrastructure.repositories"
)
@EnableKafka
@EnableCaching
@EnableRetry
@EnableAsync
@EnableTransactionManagement
public class OrderCreationApplication {

    /**
     * Main method to start the Order Creation Shred application.
     *
     * @param args command line arguments
     */
    public static void main(String[] args) {
        SpringApplication.run(OrderCreationApplication.class, args);
    }
    
    /**
     * Application startup configuration and health checks.
     */
    @Bean
    @ConditionalOnProperty(name = "app.startup.health-check.enabled", havingValue = "true", matchIfMissing = true)
    public ApplicationStartupHealthCheck startupHealthCheck() {
        return new ApplicationStartupHealthCheck();
    }
    
    /**
     * Configuration for application-level metrics and monitoring.
     */
    @Bean
    public ApplicationMetricsConfiguration metricsConfiguration() {
        return new ApplicationMetricsConfiguration();
    }
}