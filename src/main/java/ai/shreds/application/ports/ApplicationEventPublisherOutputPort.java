package ai.shreds.application.ports;

import ai.shreds.shared.dtos.SharedOrderCreatedEventDTO;
import ai.shreds.shared.dtos.SharedOrderCreationFailedEventDTO;

public interface ApplicationEventPublisherOutputPort {
    void publishOrderCreated(SharedOrderCreatedEventDTO event);
    void publishOrderCreationFailed(SharedOrderCreationFailedEventDTO event);
}
