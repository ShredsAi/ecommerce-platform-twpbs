package ai.shreds.application.ports;

import ai.shreds.shared.dtos.SharedNotificationDTO;

import java.util.List;

/**
 * Application-level output port for sending notifications.  
 * This interface is kept for backward-compatibility with components that still
 * depend on the old contract. New code should rely on
 * DomainOutputPortNotificationService instead.
 */
public interface ApplicationNotificationOutputPort {

    /**
     * Sends a single notification.
     * @param notification notification payload
     */
    void sendNotification(SharedNotificationDTO notification);

    /**
     * Sends a collection of notifications in bulk.
     * @param notifications list of notifications
     */
    void sendBulkNotifications(List<SharedNotificationDTO> notifications);
}