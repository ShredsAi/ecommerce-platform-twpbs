package ai.shreds;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.context.annotation.ComponentScan;

/**
 * Main entry point for the Payment Processing Shred application.
 * <p>
 * This microservice is responsible for managing payment intents, processing payments
 * through various processors (Stripe, PayPal, Square), handling 3D Secure flows,
 * and publishing payment events to downstream systems.
 * </p>
 */
@SpringBootApplication
@EnableKafka           // Enable Kafka for consuming OrderPlaced events and publishing payment events
@EnableAsync           // Enable async execution for non-blocking operations
@EnableScheduling      // Enable scheduling for tasks like expiring intents and polling webhook correlations
@ComponentScan(basePackages = {"ai.shreds.adapter", "ai.shreds.application", 
                             "ai.shreds.domain", "ai.shreds.infrastructure", "ai.shreds.shared"})
public class PaymentProcessingApplication {
    
    /**
     * Application main method.
     *
     * @param args command line arguments
     */
    public static void main(String[] args) {
        SpringApplication.run(PaymentProcessingApplication.class, args);
    }
}
