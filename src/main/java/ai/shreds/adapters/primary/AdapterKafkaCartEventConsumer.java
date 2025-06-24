package ai.shreds.adapters.primary;

import ai.shreds.application.ports.ApplicationCreateOrderInputPort;
import ai.shreds.adapters.exceptions.AdapterKafkaConsumerException;
import ai.shreds.shared.dtos.SharedCartCheckedOutEventDTO;
import ai.shreds.application.dtos.ApplicationOrderCreationRequestDTO;
import ai.shreds.application.dtos.ApplicationOrderCreationResponseDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class AdapterKafkaCartEventConsumer {
    
    private static final Logger logger = LoggerFactory.getLogger(AdapterKafkaCartEventConsumer.class);
    
    private final ApplicationCreateOrderInputPort applicationPort;
    private final ObjectMapper objectMapper;
    private final Map<String, Boolean> processedCartIds = new ConcurrentHashMap<>();

    public AdapterKafkaCartEventConsumer(ApplicationCreateOrderInputPort applicationPort,
                                         ObjectMapper objectMapper) {
        this.applicationPort = applicationPort;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "shopping-cart-events", groupId = "order-creation-svc")
    public void consumeCartCheckedOutEvent(@Payload String message,
                                          @Header(KafkaHeaders.RECEIVED_KEY) String key,
                                          @Header Map<String, Object> headers,
                                          Acknowledgment acknowledgment) {
        logger.info("Received CartCheckedOut event with key: {}", key);
        
        try {
            SharedCartCheckedOutEventDTO dto = mapToDTO(message);
            
            if (handleIdempotency(dto.getCartId())) {
                logger.info("Processing cart checkout for cartId: {}, customerId: {}", 
                           dto.getCartId(), dto.getCustomerId());
                
                ApplicationOrderCreationRequestDTO request = dto.toApplicationDTO();
                ApplicationOrderCreationResponseDTO response = applicationPort.execute(request);
                
                logger.info("Successfully processed cart checkout for cartId: {}, created orderId: {}", 
                           dto.getCartId(), response.getOrderId());
                
                // Mark as processed after successful execution
                markAsProcessed(dto.getCartId());
            } else {
                logger.warn("Skipping duplicate cart checkout event for cartId: {}", dto.getCartId());
            }
            
            // Acknowledge the message after successful processing
            if (acknowledgment != null) {
                acknowledgment.acknowledge();
            }
            
        } catch (Exception e) {
            logger.error("Error consuming CartCheckedOut event with key: {}", key, e);
            throw new AdapterKafkaConsumerException(
                "Error consuming CartCheckedOut event", e,
                "KAFKA_CONSUMER_ERROR", key
            );
        }
    }

    private SharedCartCheckedOutEventDTO mapToDTO(String avroMessage) {
        try {
            logger.debug("Mapping Avro message to SharedCartCheckedOutEventDTO");
            return objectMapper.readValue(avroMessage, SharedCartCheckedOutEventDTO.class);
        } catch (Exception e) {
            logger.error("Error deserializing Kafka message: {}", avroMessage, e);
            throw new AdapterKafkaConsumerException(
                "Error mapping Avro message to SharedCartCheckedOutEventDTO", e,
                "DESERIALIZATION_ERROR", null
            );
        }
    }

    private boolean handleIdempotency(String cartId) {
        // Check if this cartId has already been processed
        // This is a simple in-memory check, in production you might want to use 
        // distributed cache or database check
        return !processedCartIds.containsKey(cartId);
    }
    
    private void markAsProcessed(String cartId) {
        processedCartIds.put(cartId, true);
        logger.debug("Marked cartId {} as processed", cartId);
    }
}