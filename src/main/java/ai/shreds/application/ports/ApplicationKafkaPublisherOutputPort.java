package ai.shreds.application.ports;

import ai.shreds.shared.dtos.SharedPaymentFailedEvent;
import ai.shreds.shared.dtos.SharedPaymentSucceededEvent;

public interface ApplicationKafkaPublisherOutputPort {

    /**
     * Publish PaymentSucceeded event to Kafka topic
     * @param event the event data
     */
    void publishPaymentSucceeded(SharedPaymentSucceededEvent event);

    /**
     * Publish PaymentFailed event to Kafka topic
     * @param event the event data
     */
    void publishPaymentFailed(SharedPaymentFailedEvent event);
}