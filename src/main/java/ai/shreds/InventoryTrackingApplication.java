package ai.shreds;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.jms.annotation.EnableJms;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Main Spring Boot application class for the Inventory Tracking Shred.
 * 
 * This application provides inventory management capabilities including:
 * - Stock level tracking and validation
 * - Safety stock rule management
 * - Low stock alert monitoring
 * - ERP synchronization
 * - Real-time inventory change notifications
 */
@SpringBootApplication
@EnableScheduling
@EnableJms
@EnableKafka
@EnableCaching
@EnableTransactionManagement
public class InventoryTrackingApplication {

    public static void main(String[] args) {
        SpringApplication.run(InventoryTrackingApplication.class, args);
    }
}
