package ai.shreds.infrastructure.config;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

import java.util.HashMap;
import java.util.Map;

/**
 * Kafka configuration for the Order Creation Shred.
 * Configures Kafka consumer, error handling strategies, and container factory.
 */
@Configuration
@EnableKafka
public class InfrastructureKafkaConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${spring.kafka.consumer.group-id}")
    private String groupId;

    @Value("${spring.kafka.consumer.auto-offset-reset}")
    private String autoOffsetReset;

    /**
     * Creates Kafka consumer factory with appropriate configuration.
     * 
     * @return ConsumerFactory for Kafka consumers
     */
    @Bean
    public ConsumerFactory<String, String> consumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, autoOffsetReset);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        return new DefaultKafkaConsumerFactory<>(props);
    }

    /**
     * Creates a default error handler that will not retry failed messages.
     * This prevents duplicate message processing in error scenarios.
     * 
     * @return DefaultErrorHandler configured to not retry failed messages
     */
    @Bean
    public DefaultErrorHandler errorHandler() {
        // Create an error handler that will not retry messages on failure
        // Use FixedBackOff with interval=0 and attemps=0 to disable retries
        DefaultErrorHandler errorHandler = new DefaultErrorHandler(
                new FixedBackOff(0L, 0L)); // No retries to avoid duplicate processing
        
        // Configure error handler to handle specific exceptions differently if needed
        // errorHandler.addNotRetryableExceptions(ApplicationOrderCreationException.class);
        
        return errorHandler;
    }

    /**
     * Creates a Kafka listener container factory with manual acknowledgment and custom error handler.
     * 
     * @return ConcurrentKafkaListenerContainerFactory for Kafka listeners
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String> kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, String> factory = 
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);
        factory.setCommonErrorHandler(errorHandler());
        return factory;
    }
}