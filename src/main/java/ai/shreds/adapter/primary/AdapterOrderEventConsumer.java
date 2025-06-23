package ai.shreds.adapter.primary;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import ai.shreds.application.ports.ApplicationProcessOrderPlacedInputPort;
import ai.shreds.shared.dtos.SharedOrderPlacedEvent;

@Component
public class AdapterOrderEventConsumer {

    private final ApplicationProcessOrderPlacedInputPort applicationProcessOrderPlacedPort;

    public AdapterOrderEventConsumer(ApplicationProcessOrderPlacedInputPort applicationProcessOrderPlacedPort) {
        this.applicationProcessOrderPlacedPort = applicationProcessOrderPlacedPort;
    }

    @KafkaListener(topics = "order-events", groupId = "payment-processing-group")
    public void onOrderPlaced(SharedOrderPlacedEvent event) {
        applicationProcessOrderPlacedPort.processOrderPlaced(event.toApplicationDTO());
    }

    public String getKafkaTopic() {
        return "order-events";
    }

    public String getConsumerGroup() {
        return "payment-processing-group";
    }
}
