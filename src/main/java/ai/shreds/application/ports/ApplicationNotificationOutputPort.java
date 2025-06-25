package ai.shreds.application.ports;

import ai.shreds.shared.dtos.SharedNotificationDTO;
import java.util.List;

/**
 * Output port for notification service interactions.
 */
public interface ApplicationNotificationOutputPort {

    /**
     * Send a single notification.
     * @param notification notification DTO
     */
    void sendNotification(SharedNotificationDTO notification);

    /**
     * Send multiple notifications in bulk.
     * @param notifications list of notification DTOs
     */
    void sendBulkNotifications(List<SharedNotificationDTO> notifications);
}
