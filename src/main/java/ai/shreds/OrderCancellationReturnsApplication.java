package ai.shreds;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Main Spring Boot Application class for Order Cancellation and Returns Service.
 * 
 * This application handles:
 * - Order cancellation workflows
 * - Product return processes
 * - Refund coordination
 * - Inventory adjustments
 * - Event publishing and consumption
 * 
 * @author Generated
 * @version 1.0
 */
@SpringBootApplication(scanBasePackages = "ai.shreds")
@EnableScheduling
@EnableTransactionManagement
public class OrderCancellationReturnsApplication {

    /**
     * Main method to start the Spring Boot application.
     * 
     * @param args command line arguments
     */
    public static void main(String[] args) {
        SpringApplication.run(OrderCancellationReturnsApplication.class, args);
    }
}
